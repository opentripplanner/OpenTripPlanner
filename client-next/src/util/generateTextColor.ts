/**
 * textColor can be black or white. White for dark colors and black for light colors.
 * Calculated based on luminance formula:
 * sqrt( 0.299*Red^2 + 0.587*Green^2 + 0.114*Blue^2 )
 */
export function generateTextColor(hexColor: string) {
  const color = decodeColor(hexColor);

  //Calculates luminance based on https://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color
  const newRed = 0.299 * Math.pow(color[0] / 255.0, 2.0);
  const newGreen = 0.587 * Math.pow(color[1] / 255.0, 2.0);
  const newBlue = 0.114 * Math.pow(color[2] / 255.0, 2.0);
  const luminance = Math.sqrt(newRed + newGreen + newBlue);

  if (luminance > 0.66) {
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
