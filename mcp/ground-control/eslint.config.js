import security from "eslint-plugin-security";

export default [
  {
    files: ["**/*.js"],
    ignores: ["node_modules/**"],
    plugins: { security },
    rules: {
      ...security.configs.recommended.rules,
      // Query params and field mappings use known keys, not user input
      "security/detect-object-injection": "off",
    },
  },
];
