// Tile IDs are preserved from connectify-new; paths are relative to this repository.
export const BEACH_TILES = ['beach_ball','beach_hat','camera','coconut','crab','ice_cream','sandal','seagull','seashell','summer_shirt','summer_shorts','sunglasses','swim_ring','tropical_fish','tropical_juice'];
export const BASE_THEMES = ['candy','ocean','forest','space'];
export const PREMIUM_THEMES = ['sakura','bakery','galaxy','meadow','fruit','autumn_harvest','magic_atelier','retro_arcade','alpine_winter'];
export const REWARD_THEMES = ['sakura','bakery','galaxy','meadow','fruit'].map(id => `reward_${id}`);
export const TILE_SETS = Object.fromEntries([...BASE_THEMES, ...PREMIUM_THEMES].map(id => [id, Array.from({length:16}, (_,i) => `${id}_${i}`)]));
for (const id of REWARD_THEMES) TILE_SETS[id] = Array.from({length:10}, (_,i) => `${id}_${i}`);
TILE_SETS.sweets = BEACH_TILES;
TILE_SETS.vanilla = BEACH_TILES;
export const tileIdsFor = theme => [...(TILE_SETS[theme] || BEACH_TILES)];
export function tileAssetFor(id) {
  if (BEACH_TILES.includes(id)) return `assets/tiles/beach/${id}.png`;
  for (const theme of [...BASE_THEMES, ...PREMIUM_THEMES, ...REWARD_THEMES]) {
    const index = TILE_SETS[theme].indexOf(id);
    if (index >= 0) return `assets/tiles/${theme}/${index}.${BASE_THEMES.includes(theme) ? 'png' : 'webp'}`;
  }
  return null;
}
