import { build } from 'esbuild';
import { mkdir, readFile, writeFile } from 'node:fs/promises';

await mkdir('dist', { recursive: true });
await build({
  entryPoints: ['src/main.tsx'],
  bundle: true,
  minify: true,
  outfile: 'dist/app.js',
  jsx: 'automatic',
  define: { 'process.env.NODE_ENV': '"production"' },
});

// Reuse the readable source HTML instead of maintaining another inline page.
const sourceHtml = await readFile('index.html', 'utf8');
const productionHtml = sourceHtml
  .replace('/src/main.tsx', '/app.js')
  .replace('</head>', '  <link rel="stylesheet" href="/app.css" />\n  </head>');
await writeFile('dist/index.html', productionHtml);
console.log('Portal production build complete');
