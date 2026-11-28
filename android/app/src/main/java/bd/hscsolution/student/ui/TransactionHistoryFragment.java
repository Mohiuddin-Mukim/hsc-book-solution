package bd.hscsolution.student.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.commerce.CommerceModels;
import bd.hscsolution.student.commerce.CommerceViewModel;
import bd.hscsolution.student.commerce.CommerceViewModelFactory;

public final class TransactionHistoryFragment extends Fragment {
    public TransactionHistoryFragment() { super(R.layout.fragment_transaction_history); }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        CommerceViewModel vm = new ViewModelProvider(this, new CommerceViewModelFactory(
                ((HscApplication) requireActivity().getApplication()).container().commerceRepository)).get(CommerceViewModel.class);

        LinearLayout list = v.findViewById(R.id.transactionList);
        TextView empty = v.findViewById(R.id.transactionEmpty);
        TextView error = v.findViewById(R.id.transactionError);
        ProgressBar progress = v.findViewById(R.id.transactionProgress);

        vm.transactions().observe(getViewLifecycleOwner(), rows -> {
            progress.setVisibility(View.GONE);
            list.removeAllViews();
            if (rows == null || rows.isEmpty()) {
                empty.setVisibility(View.VISIBLE);
                return;
            }
            empty.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (CommerceModels.Order o : rows) {
                View card = inflater.inflate(R.layout.item_transaction, list, false);
                ((TextView) card.findViewById(R.id.txPlanName)).setText(o.planName());
                ((TextView) card.findViewById(R.id.txAmount)).setText(
                        String.format(Locale.US, "৳%.2f", o.amountMinor() / 100.0));
                ((TextView) card.findViewById(R.id.txStatus)).setText(statusLabel(o));
                ((TextView) card.findViewById(R.id.txDate)).setText(o.paidAt() == null ? o.createdAt() : o.paidAt());
                String trx = o.transactionId() == null ? "Pending" : o.transactionId();
                String method = o.paymentMethod() == null ? "—" : o.paymentMethod();
                String source = o.source() == null ? "—" : o.source();
                String ref = o.referenceCode() == null ? "—" : o.referenceCode();
                ((TextView) card.findViewById(R.id.txId)).setText(
                        "TrxID: " + trx + "  •  Method: " + method);
                ((TextView) card.findViewById(R.id.txValidity)).setText(
                        "Reference: " + ref + "  •  " + source + "  •  Access: " +
                        (o.expiresAt() == null ? "—" : o.expiresAt()));
                list.addView(card);
            }
        });

        vm.error().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isBlank()) {
                progress.setVisibility(View.GONE);
                error.setText(msg);
                error.setVisibility(View.VISIBLE);
            }
        });

        progress.setVisibility(View.VISIBLE);
        vm.loadTransactions();
    }

    private String statusLabel(CommerceModels.Order o) {
        if ("ACTIVE".equalsIgnoreCase(o.entitlementStatus()) && "PAID".equalsIgnoreCase(o.status()))
            return "Paid • Active";
        if ("PAID".equalsIgnoreCase(o.status())) return "Paid";
        if ("FAILED".equalsIgnoreCase(o.status())) return "Failed";
        return "Pending";
    }
}
