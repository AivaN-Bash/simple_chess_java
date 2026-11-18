import { defineConfig } from "vite";

const githubRepository = process.env.GITHUB_REPOSITORY;
let base = "/";
if (process.env.GITHUB_ACTIONS === "true" && githubRepository) {
  const repositoryName = githubRepository.split("/")[1];
  if (!repositoryName.endsWith(".github.io")) base = `/${repositoryName}/`;
}

export default defineConfig({ base });
