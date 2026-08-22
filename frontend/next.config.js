/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // The repository contains legacy tracked .next output. Keep local builds
  // isolated so stale generated bundles cannot crash the dev server.
  distDir: ".next-local",
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "images.unsplash.com" },
      { protocol: "https", hostname: "api.dicebear.com" },
    ],
  },
};

module.exports = nextConfig;
