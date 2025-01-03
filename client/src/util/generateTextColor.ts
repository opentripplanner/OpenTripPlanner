/**
 * textColor can be black or white. White for dark colors and black for light colors.
 * Calculated according to WCAG 2.1
 */
export function generateTextColor(hexColor: string) {
  const color = decodeColor(hexColor);

  function linearizeColorComponent(srgb: number) {
    return srgb <= 0.04045 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
  }

  const r = linearizeColorComponent(color[0] / 255.0);
  const g = linearizeColorComponent(color[1] / 255.0);
  const b = linearizeColorComponent(color[2] / 255.0);
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;

  if (luminance > 0.179) {
    return '#000';
  } else {
    return '#fff';
  }
}

function decodeColor(hex: string): number[] {
  return hex2rgb(hex);
}

function hex2rgb(hex: string) {
  if (hex.length === 4) {
    return fullHex(hex);
  }

  return [parseInt(hex.slice(1, 3), 16), parseInt(hex.slice(3, 5), 16), parseInt(hex.slice(5, 7), 16)];
}

function fullHex(hex: string) {
  const r = hex.slice(1, 2);
  const g = hex.slice(2, 3);
  const b = hex.slice(3, 4);

  return [parseInt(r + r, 16), parseInt(g + g, 16), parseInt(b + b, 16)];
}
