import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { ToastProvider } from "./components/ui/Toast";
import { LlmProviderProvider } from "./hooks/LlmProviderProvider";
import "./index.css";

const rootElement = document.getElementById("root");
if (!rootElement) {
  throw new Error("Root element not found");
}

createRoot(rootElement).render(
  <StrictMode>
    <ToastProvider>
      <LlmProviderProvider>
        <App />
      </LlmProviderProvider>
    </ToastProvider>
  </StrictMode>,
);
