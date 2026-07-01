import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { LlmProviderProvider } from "./hooks/LlmProviderProvider";
import "./index.css";

const rootElement = document.getElementById("root");
if (!rootElement) {
  throw new Error("Root element not found");
}

createRoot(rootElement).render(
  <StrictMode>
    <LlmProviderProvider>
      <App />
    </LlmProviderProvider>
  </StrictMode>,
);
