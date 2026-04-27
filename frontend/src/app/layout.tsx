import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "AI English Tutor",
  description: "AI news curation & English intensive-reading learning assistant",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <header className="border-b bg-background">
          <div className="mx-auto flex h-14 max-w-5xl items-center gap-6 px-4">
            <Link href="/" className="text-lg font-semibold tracking-tight">
              AI English Tutor
            </Link>
            <nav className="flex items-center gap-4 text-sm">
              <Link
                href="/"
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                Today
              </Link>
              <Link
                href="/history"
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                History
              </Link>
            </nav>
          </div>
        </header>
        <main className="flex-1">
          <div className="mx-auto max-w-5xl px-4 py-6">{children}</div>
        </main>
      </body>
    </html>
  );
}
