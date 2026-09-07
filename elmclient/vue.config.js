const authenticationPreview = process.env.ELM_AUTH_PREVIEW === 'true';

module.exports = {
  productionSourceMap: false,
  ...(authenticationPreview ? {
    pages: { index: { entry: 'src/auth-preview/main.js', template: 'public/index.html', title: '账号功能联调' } },
    outputDir: 'dist-auth'
  } : {}),
  devServer: {
    port: 8080
  }
};
