import katex from "katex";
import "katex/dist/katex.min.css";

export function MathText({ text }: { text: string }) {
  if (!text) return null;

  // \(...\), \[...\], $$...$$, এবং $...$ আলাদা করার রেজেক্স
  const parts = text.split(/(\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\]|\\\([\s\S]+?\\\)|\$[^$\n]+?\$)/g);

  return (
    <div style={{ whiteSpace: "pre-wrap" }}>
      {parts.map((part, index) => {
        if (part.startsWith("$$") && part.endsWith("$$")) {
          const html = katex.renderToString(part.slice(2, -2), { displayMode: true, throwOnError: false });
          return <span key={index} dangerouslySetInnerHTML={{ __html: html }} />;
        }
        if (part.startsWith("\\[") && part.endsWith("\\]")) {
          const html = katex.renderToString(part.slice(2, -2), { displayMode: true, throwOnError: false });
          return <span key={index} dangerouslySetInnerHTML={{ __html: html }} />;
        }
        if (part.startsWith("\\(") && part.endsWith("\\)")) {
          const html = katex.renderToString(part.slice(2, -2), { displayMode: false, throwOnError: false });
          return <span key={index} dangerouslySetInnerHTML={{ __html: html }} />;
        }
        if (part.startsWith("$") && part.endsWith("$")) {
          const html = katex.renderToString(part.slice(1, -1), { displayMode: false, throwOnError: false });
          return <span key={index} dangerouslySetInnerHTML={{ __html: html }} />;
        }
        return <span key={index}>{part}</span>;
      })}
    </div>
  );
}
export default MathText;