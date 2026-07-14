import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ResetPasswordPage } from "./ResetPasswordPage";

vi.mock("../api/client");
const client = vi.mocked(await import("../api/client"));

describe("ResetPasswordPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  function fillPasswords(newPassword: string, confirm: string) {
    fireEvent.change(screen.getByLabelText("New password"), { target: { value: newPassword } });
    fireEvent.change(screen.getByLabelText("Confirm new password"), {
      target: { value: confirm },
    });
  }

  it("rejects mismatched passwords without calling the API", async () => {
    render(<ResetPasswordPage navigate={vi.fn()} token="reset-token" />);

    fillPasswords("NewPassword1!", "Different1!");
    fireEvent.click(screen.getByRole("button", { name: "Update password" }));

    expect(await screen.findByText("Passwords do not match.")).toBeInTheDocument();
    expect(client.resetPassword).not.toHaveBeenCalled();
  });

  it("submits the token and new password, then shows the success screen", async () => {
    client.resetPassword.mockResolvedValue(undefined);
    render(<ResetPasswordPage navigate={vi.fn()} token="reset-token" />);

    fillPasswords("NewPassword1!", "NewPassword1!");
    fireEvent.click(screen.getByRole("button", { name: "Update password" }));

    await waitFor(() => expect(screen.getByText("Password updated")).toBeInTheDocument());
    expect(client.resetPassword).toHaveBeenCalledWith("reset-token", "NewPassword1!");
  });

  it("shows the backend's error message and lets the user retry", async () => {
    client.resetPassword.mockRejectedValue(new Error("This reset link has expired."));
    render(<ResetPasswordPage navigate={vi.fn()} token="reset-token" />);

    fillPasswords("NewPassword1!", "NewPassword1!");
    fireEvent.click(screen.getByRole("button", { name: "Update password" }));

    expect(await screen.findByText("This reset link has expired.")).toBeInTheDocument();
    // Still on the form, not the success screen, and free to retry.
    expect(screen.getByRole("button", { name: "Update password" })).not.toBeDisabled();
  });

  it("navigates to /login when 'Go to login' is clicked after success", async () => {
    client.resetPassword.mockResolvedValue(undefined);
    const navigate = vi.fn();
    render(<ResetPasswordPage navigate={navigate} token="reset-token" />);

    fillPasswords("NewPassword1!", "NewPassword1!");
    fireEvent.click(screen.getByRole("button", { name: "Update password" }));
    await waitFor(() => expect(screen.getByText("Password updated")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Go to login" }));

    expect(navigate).toHaveBeenCalledWith("/login");
  });
});
