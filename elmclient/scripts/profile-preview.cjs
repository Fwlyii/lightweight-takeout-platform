const command = process.argv[2];
if (!["serve", "build"].includes(command))
  throw new Error("Expected serve or build");
process.env.ELM_PROFILE_PREVIEW = "true";
process.env.VUE_APP_API_BASE_URL ||= "http://localhost:18084";
const cli = require.resolve("@vue/cli-service/bin/vue-cli-service");
process.argv = [
  process.execPath,
  cli,
  command,
  ...(command === "serve" ? ["--port", "18085", "--host", "127.0.0.1"] : []),
];
require(cli);
