// SPDX-License-Identifier: MPL-2.0

// Production builds also write a maximum-quality Brotli copy of each Wasm module next to it (`<module>.wasm.br`).
// Cloudflare Pages compresses on the fly at a low Brotli level; the Pages worker (`_worker.js`) serves these copies
// instead, which are about a fifth smaller. Uses Node's built-in zlib, so it adds no dependency.
;(function (config) {
    if (config.mode !== 'production') return;
    const zlib = require('zlib');
    config.plugins.push({
        apply(compiler) {
            const { Compilation, sources } = compiler.webpack;
            compiler.hooks.thisCompilation.tap('BrotliWasm', (compilation) => {
                compilation.hooks.processAssets.tap(
                    { name: 'BrotliWasm', stage: Compilation.PROCESS_ASSETS_STAGE_OPTIMIZE_TRANSFER },
                    (assets) => {
                        for (const name of Object.keys(assets).filter((asset) => asset.endsWith('.wasm'))) {
                            const module = assets[name].buffer();
                            const compressed = zlib.brotliCompressSync(module, {
                                params: {
                                    [zlib.constants.BROTLI_PARAM_QUALITY]: zlib.constants.BROTLI_MAX_QUALITY,
                                    // A 16 MiB window, the largest every Brotli decoder supports.
                                    [zlib.constants.BROTLI_PARAM_LGWIN]: 24,
                                    [zlib.constants.BROTLI_PARAM_SIZE_HINT]: module.length,
                                },
                            });
                            compilation.emitAsset(`${name}.br`, new sources.RawSource(compressed));
                        }
                    },
                );
            });
        },
    });
})(config);
