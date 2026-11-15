import { useState } from "react";
import MediaUploader from "../components/MediaUploader";
export default function MediaLibraryPage() {
  const [id, setId] = useState("");
  const [type, setType] = useState<"questionVersion" | "solutionStep">(
    "questionVersion",
  );
  return (
    <div>
      <div className="page-head">
        <div>
          <span className="eyebrow">MEDIA</span>
          <h1>Media Library</h1>
          <p>
            Upload valid figures and attach them to a draft question version or
            exact solution step.
          </p>
        </div>
      </div>
      <div className="panel">
        <div className="filters two">
          <label>
            Target type
            <select
              value={type}
              onChange={(e) => setType(e.target.value as any)}
            >
              <option value="questionVersion">Question version</option>
              <option value="solutionStep">Solution step</option>
            </select>
          </label>
          <label>
            Target ID
            <input
              value={id}
              onChange={(e) => setId(e.target.value)}
              placeholder="UUID"
            />
          </label>
        </div>
        {id && <MediaUploader targetType={type} targetId={id} />}
      </div>
    </div>
  );
}
