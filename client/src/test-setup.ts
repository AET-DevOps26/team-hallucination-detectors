import "@testing-library/jest-dom";

// Node's built-in localStorage (stable since Node 22.4) is installed on
// globalThis before jsdom runs. vitest's jsdom environment won't overwrite a
// global that already exists, so window.localStorage would silently resolve
// to Node's stub instead of jsdom's Storage. Disabled via NODE_OPTIONS in the
// "test" npm script (client/package.json) so jsdom can install its own.