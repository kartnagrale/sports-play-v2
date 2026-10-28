import nextVitals from "eslint-config-next/core-web-vitals";
export default [
  { ignores: [".next/**", ".next-local/**", "node_modules/**", "next-env.d.ts"] },
  ...nextVitals,
];
