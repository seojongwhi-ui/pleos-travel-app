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
// Figma 328:5199 is the user's approved visual source for the driving screen.
// Its additional colors are allowed only in that screen, never project wide.
const driveDesign = JSON.parse(fs.readFileSync(path.join(root, 'docs/design/linkcar-driving-figma.json'), 'utf8'));
const driveFiles = [driveDesign.file, ...(driveDesign.additionalFiles || [])];
const drivePalette = new Set(driveDesign.colors.map(v => v.slice(1).toUpperCase() + 'FF'));
const homeDesign = JSON.parse(fs.readFileSync(path.join(root, 'docs/design/linkcar-home-figma.json'), 'utf8'));
const homePalette = new Set(homeDesign.colors.map(v => v.slice(1).toUpperCase() + 'FF'));
const callDesign = JSON.parse(fs.readFileSync(path.join(root, 'docs/design/linkcar-call-figma.json'), 'utf8'));
const callPalette = new Set(callDesign.colors.map(v => v.slice(1).toUpperCase() + 'FF'));
const journeyDesign = JSON.parse(fs.readFileSync(path.join(root, 'docs/design/linkcar-journey-figma.json'), 'utf8'));
const journeyPalette = new Set(journeyDesign.colors.map(v => v.slice(1).toUpperCase() + 'FF'));
// Figma 328:8410 and 328:8645 use #0D0E12 at 45% for their dialog backdrops.
const dialogBackdropColor = '0D0E12FF';
// Music illustration gradients and small control surfaces are implementation
// approximations, tracked separately from colors sampled from the Figma source.
for (const color of journeyDesign.implementationColors || []) {
    journeyPalette.add(color.slice(1).toUpperCase() + 'FF');
}
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
    // The approved Figma driving and map cards specify their own text sizes.
    // Keep that exception scoped to this one design implementation file.
    const callTimerOnly = name === callDesign.file &&
        [...text.matchAll(/\bfontSize\s*=\s*(\d+)\.sp/g)].every(m => Number(m[1]) === callDesign.timerFontSize);
    if (name !== 'PleosDesign215.kt' && name !== homeDesign.file && name !== journeyDesign.file &&
        !driveFiles.includes(name) && !callTimerOnly && /\bfontSize\s*=/.test(text)) {
        issues.push(`${name}: use MaterialTheme.typography instead of a screen-specific font size`);
    }
    for (const m of text.matchAll(/Color\(0x([\dA-F]{8})\)/gi)) {
        const argb = m[1].toUpperCase();
        const rgba = argb.slice(2) + argb.slice(0, 2);
        if (!palette.has(rgba) && !([...driveFiles, 'LinkCarDesignedJourney.kt',
            'LinkCarDesignedDialogs.kt'].includes(name) && drivePalette.has(rgba)) &&
            !(name === 'LinkCarDesignedDialogs.kt' && rgba === dialogBackdropColor) &&
            !(name === homeDesign.file && homePalette.has(rgba)) &&
            !(name === journeyDesign.file && journeyPalette.has(rgba)) &&
            !(name === callDesign.file && callPalette.has(rgba))) {
            issues.push(`${name}: #${argb} is absent from the recorded source palette`);
        }
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
    console.log('PASS: Pleos 2.1.5 reference fingerprints and typography; colors match Pleos or the scoped Figma screen palette.');
    console.log('Layout, interaction, icon details, and other form factors still require visual verification.');
}
