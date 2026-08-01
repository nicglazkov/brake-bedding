import type { NextConfig } from "next";

// The site exports as static files for GitHub Pages, under the project path.
const nextConfig: NextConfig = {
  output: "export",
  basePath: "/brake-bedding",
  images: { unoptimized: true },
  trailingSlash: false,
};

export default nextConfig;
