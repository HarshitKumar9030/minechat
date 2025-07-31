import type { Metadata } from "next";
import { Inter } from "next/font/google";
import localFont from "next/font/local";
import "./globals.css";
import { SERVER_DESCRIPTION, SERVER_NAME } from '@/lib/constants'
import NavBar from "@/components/NavBar";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  weight: ["100", "200", "300", "400", "500", "600", "700", "800", "900"],
})

const minecrafter = localFont({
  src: [
    {
      path: "../../public/fonts/Minecrafter.Reg.ttf",
      weight: "400",
      style: "normal",
    },
    {
      path: "../../public/fonts/Minecrafter.Alt.ttf", 
      weight: "700",
      style: "normal",
    },
  ],
  variable: "--font-minecrafter",
  display: "swap",
});

const minecraftia = localFont({
  src: "../../public/fonts/Minecraftia-Regular.ttf",
  variable: "--font-minecraftia",
  weight: "400",
  display: "swap",
});

export const metadata: Metadata = {
  title: SERVER_NAME,
  description: SERVER_DESCRIPTION,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${inter.variable} ${minecrafter.variable} ${minecraftia.variable} ${inter.className} antialiased bg-background`}
      >
        <NavBar />
        {children}
      </body>
    </html>
  );
}
