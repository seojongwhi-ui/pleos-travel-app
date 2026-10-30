// Offline lint against the user's extracted 2.1.5 Figma reference, not a UI screenshot test.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const spec = JSON.parse(fs.readFileSync(path.join(root, 'docs/design/pleos-215-tokens.json'), 'utf8'));
const dir = path.join(root, 'travel-preview/src/main/java/com/contest/travel/preview');
const issues = [];
const palette = new Set(spec.variables.flatMap(v => (v.values || []).map(m => m.value)
    .filter(v => typeof v === 'string' && v.startsWith('#')).map(v => v.slice(1).toUpperCase())));
const typePairs = new Set(spec.typography.map(t => `${t.size}/${t.lineHeight.value}`));

for (const source of spec.sources) {
    const file = path.join(root, 'design/reference', source.file);
    if (fs.existsSync(file)) {
        const hash = crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
        if (hash !== source.sha256) issues.push(`${source.file}: original differs from the recorded reference`);
    }
}

for (const name of fs.readdirSync(dir).filter(f => f.endsWith('.kt'))) {
    const text = fs.readFileSync(path.join(dir, name), 'utf8');
    if (name !== 'PleosDesign215.kt' && /\bfontSize\s*=/.test(text)) {
        issues.push(`${name}: use MaterialTheme.typography instead of a screen-specific font size`);
    }
    for (const m of text.matchAll(/Color\(0x([\dA-F]{8})\)/gi)) {
        const argb = m[1].toUpperCase();
        const rgba = argb.slice(2) + argb.slice(0, 2);
        if (!palette.has(rgba)) issues.push(`${name}: #${argb} is absent from the recorded source palette`);
    }
}
const theme = fs.readFileSync(path.join(dir, 'PleosDesign215.kt'), 'utf8');
for (const m of theme.matchAll(/\btype\((\d+),\s*(\d+)/g)) {
    if (!typePairs.has(`${m[1]}/${m[2]}`)) issues.push(`PleosDesign215.kt: ${m[1]}/${m[2]} is absent from source typography`);
}
if (issues.length) {
    console.error(issues.join('\n'));
    process.exitCode = 1;
} else {
    console.log('PASS: reference fingerprints, app color literals, and typography match the recorded Pleos 2.1.5 source.');
    console.log('Layout, interaction, icon details, and other form factors still require visual verification.');
}
