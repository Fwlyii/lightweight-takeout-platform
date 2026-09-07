const authenticationPreview = process.env.ELM_AUTH_PREVIEW === 'true';
const profilePreview = process.env.ELM_PROFILE_PREVIEW === 'true';

module.exports = {
  productionSourceMap: false,
  ...(profilePreview ? {
    pages: { index: { entry: 'src/profile-preview/main.js', template: 'public/index.html', title: '个人资料与地址联调' } },
    outputDir: 'dist-profile'
  } : {}),
  ...(authenticationPreview ? {
    pages: { index: { entry: 'src/auth-preview/main.js', template: 'public/index.html', title: '账号功能联调' } },
    outputDir: 'dist-auth'
  } : {}),
  devServer: {
    port: 8080
  }
};
