import type { Metadata } from "next";
export const metadata: Metadata = { title: "Account activation", robots: { index: false, follow: false } };
export default function ActivationLayout({ children }: { children: React.ReactNode }) { return children; }
