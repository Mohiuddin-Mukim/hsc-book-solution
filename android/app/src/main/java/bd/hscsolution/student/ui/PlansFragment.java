package bd.hscsolution.student.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import bd.hscsolution.student.BuildConfig;
import bd.hscsolution.student.HscApplication;
import bd.hscsolution.student.R;
import bd.hscsolution.student.commerce.CommerceModels;
import bd.hscsolution.student.commerce.CommerceViewModel;
import bd.hscsolution.student.commerce.CommerceViewModelFactory;

public final class PlansFragment extends Fragment {
    private CommerceViewModel vm;
    private TextView err;

    public PlansFragment() { super(R.layout.fragment_plans); }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        vm = new ViewModelProvider(this, new CommerceViewModelFactory(
                ((HscApplication) requireActivity().getApplication()).container().commerceRepository)).get(CommerceViewModel.class);

        LinearLayout list = v.findViewById(R.id.planList);
        err = v.findViewById(R.id.planError);
        MaterialButton historyButton = v.findViewById(R.id.transactionHistoryButton);
        historyButton.setOnClickListener(x -> Navigation.findNavController(x).navigate(R.id.action_plans_to_transactions));

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        vm.plans().observe(getViewLifecycleOwner(), ps -> {
            list.removeAllViews();
            if (ps == null) return;

            for (CommerceModels.StudentPlan p : ps) {
                View card = inflater.inflate(R.layout.item_plan_card, list, false);
                TextView name = card.findViewById(R.id.planName);
                TextView discount = card.findViewById(R.id.planDiscount);
                TextView sale = card.findViewById(R.id.planSalePrice);
                TextView original = card.findViewById(R.id.planOriginalPrice);
                TextView desc = card.findViewById(R.id.planDescription);
                TextView access = card.findViewById(R.id.planAccessStatus);
                MaterialButton buy = card.findViewById(R.id.planBuyButton);

                name.setText(p.name());
                sale.setText(formatTaka(p.salePriceMinor()));

                if (p.originalPriceMinor() > p.salePriceMinor()) {
                    original.setText(formatTaka(p.originalPriceMinor()));
                    original.setPaintFlags(original.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    original.setVisibility(View.VISIBLE);
                    discount.setText(String.format(Locale.US, "%.0f%% OFF", p.discountPercent()));
                    discount.setVisibility(View.VISIBLE);
                }

                if (p.description() != null && !p.description().isBlank()) {
                    desc.setText(p.description());
                    desc.setVisibility(View.VISIBLE);
                }

                boolean paid = "PAID".equalsIgnoreCase(p.accessStatus());
                if (paid) {
                    access.setText("Paid • Active");
                    access.setVisibility(View.VISIBLE);
                    buy.setText("Accessible");
                    buy.setEnabled(false);
                    buy.setAlpha(0.65f);
                    card.setOnClickListener(x -> {});
                } else {
                    access.setVisibility(View.GONE);
                    buy.setText("Choose plan");
                    buy.setEnabled(true);
                    buy.setAlpha(1f);
                    View.OnClickListener action = x -> vm.buy(p.id());
                    card.setOnClickListener(action);
                    buy.setOnClickListener(action);
                }

                list.addView(card);
            }
        });

        vm.error().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isBlank()) {
                err.setVisibility(View.GONE);
            } else {
                err.setText(msg);
                err.setVisibility(View.VISIBLE);
            }
        });

        vm.manualPortalUrl().observe(getViewLifecycleOwner(), rawUrl -> {
            if (rawUrl == null || rawUrl.isBlank()) return;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)));
            } catch (ActivityNotFoundException e) {
                err.setText("No browser found to open the payment portal.");
                err.setVisibility(View.VISIBLE);
            }
        });


        vm.load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.load();
    }

    private String formatTaka(long minorAmount) {
        return String.format(Locale.US, "৳%.2f", minorAmount / 100.0);
    }

    private String resolveCheckoutUrl(String rawUrl) {
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        Uri baseUri = Uri.parse(BuildConfig.API_BASE_URL);
        String origin = baseUri.getScheme() + "://" + baseUri.getAuthority();
        return trimmed.startsWith("/") ? origin + trimmed : origin + "/" + trimmed;
    }
}
