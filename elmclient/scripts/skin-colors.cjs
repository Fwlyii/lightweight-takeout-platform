// Shared by the theme migration audit and regression tests. Semantic reds,
// greens and yellows are deliberately not classified as platform blue.
function skinColor(literal) {
  if (literal.includes('var(')) return null;
  let channels;
  if (literal.startsWith('#')) {
    let hex = literal.slice(1);
    if (hex.length === 3 || hex.length === 4) hex = [...hex].map(c => c + c).join('');
    channels = hex.match(/../g).map(c => parseInt(c, 16));
    if (channels.length === 4) channels[3] /= 255;
  } else channels = literal.match(/[\d.]+/g).map(Number);
  const [r, g, b] = channels.map(c => c / 255);
  const max = Math.max(r, g, b), min = Math.min(r, g, b), delta = max - min;
  if (!delta) return null;
  const light = (max + min) / 2;
  const saturation = delta / (1 - Math.abs(2 * light - 1));
  const hue = (max === r ? (g - b) / delta + (g < b ? 6 : 0) : max === g ? (b - r) / delta + 2 : (r - g) / delta + 4) * 60;
  if (hue < 175 || hue > 255 || saturation < .08) return null;
  let token;
  if (light >= .94) token = 'surface';
  else if (light >= .84) token = 'border';
  else if (saturation >= .45) token = light < .39 ? 'brand-strong' : light < .65 ? 'brand' : 'brand-soft';
  else token = light < .36 ? 'ink' : light < .66 ? 'muted' : 'subtle';
  return { token, channels };
}
module.exports = { skinColor };
