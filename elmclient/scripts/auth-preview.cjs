// Separate entry for testing account features before the other business pages are available.
const command = process.argv[2];
if (!['serve', 'build'].includes(command)) throw new Error('Expected serve or build');
process.env.ELM_AUTH_PREVIEW = 'true';
process.env.VUE_APP_API_BASE_URL ||= 'http://localhost:18082';
const cli = require.resolve('@vue/cli-service/bin/vue-cli-service');
process.argv = [process.execPath, cli, command, ...(command === 'serve' ? ['--port', '18083', '--host', '127.0.0.1'] : [])];
require(cli);
