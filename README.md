# textarea-to-code-editor

[![Build Status](https://travis-ci.org/nvbn/textarea-to-code-editor.svg)](https://travis-ci.org/nvbn/textarea-to-code-editor)

[![Available in the chrome web store](https://developer.chrome.com/webstore/images/ChromeWebStore_Badge_v2_206x58.png)](https://chrome.google.com/webstore/detail/kcapdaijpdnhajjgdimlhoaaaiplkobj)

Chrome extension for converting textarea to code editor

## Building

For building local version of extensions you should run:

```bash
lein bower install
lein cljsbuild once
```

And install unpacked extension from `resources`.

For running tests:

```bash
lein cljsbuild test
```
