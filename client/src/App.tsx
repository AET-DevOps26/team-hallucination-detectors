import { FormEvent, useEffect, useState } from "react";
import { fetchCurrentUser, fetchHello, loginUser, registerUser } from "./api/client";

type AuthMode = "login" | "register";

export default function App() {
  const [mode, setMode] = useState<AuthMode>("login");
  const [apiMessage, setApiMessage] = useState("Checking API...");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [currentUserEmail, setCurrentUserEmail] = useState<string | null>(null);
  const [authMessage, setAuthMessage] = useState("");
  const [authError, setAuthError] = useState("");

  useEffect(() => {
    fetchHello()
      .then((res) => setApiMessage(res.message))
      .catch((err: unknown) => {
        const error = err instanceof Error ? err.message : "Request failed";
        setApiMessage(`Could not reach API: ${error}`);
      });
  }, []);

  useEffect(() => {
    const token = localStorage.getItem("authToken");

    if (!token) {
      return;
    }

    fetchCurrentUser(token)
      .then((user) => setCurrentUserEmail(user.email))
      .catch(() => {
        localStorage.removeItem("authToken");
        setCurrentUserEmail(null);
      });
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setAuthMessage("");
    setAuthError("");

    try {
      if (mode === "register") {
        await registerUser(email, password);
        setAuthMessage("Registration successful. You can now log in.");
        setMode("login");
        return;
      }

      const response = await loginUser(email, password);
      localStorage.setItem("authToken", response.token);
      setCurrentUserEmail(response.email);
      setAuthMessage("Login successful.");
      setEmail("");
      setPassword("");
    } catch {
      setAuthError("Authentication failed. Please check your email and password.");
    }
  }

  function handleLogout() {
    localStorage.removeItem("authToken");
    setCurrentUserEmail(null);
    setAuthMessage("Logged out successfully.");
    setAuthError("");
  }

  return (
    <div className="min-h-full flex items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-md p-8">
        <h1 className="text-3xl font-semibold text-slate-900 mb-2 text-center">
          VibeShield
        </h1>

        <p className="mb-6 text-center text-slate-600">{apiMessage}</p>

        {currentUserEmail ? (
          <div className="rounded-xl border border-slate-200 p-5 text-center">
            <p className="text-sm text-slate-500 mb-1">Logged in as</p>
            <p className="font-medium text-slate-900 mb-4">{currentUserEmail}</p>

            <button
              type="button"
              onClick={handleLogout}
              className="w-full rounded-lg bg-slate-900 px-4 py-2 text-white font-medium hover:bg-slate-700"
            >
              Logout
            </button>
          </div>
        ) : (
          <>
            <div className="mb-5 flex rounded-lg bg-slate-100 p-1">
              <button
                type="button"
                onClick={() => setMode("login")}
                className={`w-1/2 rounded-md py-2 text-sm font-medium ${
                  mode === "login"
                    ? "bg-white text-slate-900 shadow-sm"
                    : "text-slate-500"
                }`}
              >
                Login
              </button>

              <button
                type="button"
                onClick={() => setMode("register")}
                className={`w-1/2 rounded-md py-2 text-sm font-medium ${
                  mode === "register"
                    ? "bg-white text-slate-900 shadow-sm"
                    : "text-slate-500"
                }`}
              >
                Register
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">
                  Email
                </label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-slate-900"
                  placeholder="user@example.com"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">
                  Password
                </label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-slate-900"
                  placeholder="At least 6 characters"
                />
              </div>

              <button
                type="submit"
                className="w-full rounded-lg bg-slate-900 px-4 py-2 text-white font-medium hover:bg-slate-700"
              >
                {mode === "login" ? "Login" : "Create account"}
              </button>
            </form>
          </>
        )}

        {authMessage && (
          <p className="mt-4 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">
            {authMessage}
          </p>
        )}

        {authError && (
          <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {authError}
          </p>
        )}
      </div>
    </div>
  );
}
