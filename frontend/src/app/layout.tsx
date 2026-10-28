import type { Metadata } from "next";
import "./globals.css";
import { Toaster } from "sonner";
import { ThemeProvider } from "@/components/theme-provider";
import { Outfit, JetBrains_Mono } from "next/font/google";

const bodyFont=Outfit({subsets:["latin"],variable:"--font-outfit",display:"swap"});
const monoFont=JetBrains_Mono({subsets:["latin"],variable:"--font-jetbrains",display:"swap"});

export const metadata: Metadata = {
  title: { default: "Play-Neml | Championship workspace", template: "%s | Play-Neml" },
  description: "One Platform. Every Championship. Manage multi-sport championships, live auctions, teams and scores with Play-Neml.",
  applicationName: "Play-Neml",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${bodyFont.variable} ${monoFont.variable} bg-bg antialiased`}>
        <ThemeProvider attribute="class" defaultTheme="dark" enableSystem={false}>
          <div className="relative z-10 min-h-screen">{children}</div>
          <Toaster
            position="top-right"
            toastOptions={{
              className: "font-body",
            }}
          />
        </ThemeProvider>
      </body>
    </html>
  );
}
