// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

class MockIntersectionObserver {
	private readonly callback: (entries: IntersectionObserverEntry[]) => void;
	constructor(cb: (entries: IntersectionObserverEntry[]) => void) {
		this.callback = cb;
	}
	observe(): void {
		// simulate immediate intersection for tests if needed
	}
	unobserve(): void {
		// noop
	}
	disconnect(): void {
		// noop
	}
	trigger(entries: Partial<IntersectionObserverEntry>[]): void {
		this.callback(entries as IntersectionObserverEntry[]);
	}
}

(
	globalThis as unknown as {
		IntersectionObserver: typeof MockIntersectionObserver;
	}
).IntersectionObserver = MockIntersectionObserver as any;

// Mock ResizeObserver which is not available in test environment
global.ResizeObserver = jest.fn().mockImplementation(() => ({
	observe: jest.fn(),
	unobserve: jest.fn(),
	disconnect: jest.fn(),
}));

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
	writable: true,
	value: jest.fn().mockImplementation((query) => ({
		matches: false,
		media: query,
		onchange: null,
		addListener: jest.fn(), // deprecated
		removeListener: jest.fn(), // deprecated
		addEventListener: jest.fn(),
		removeEventListener: jest.fn(),
		dispatchEvent: jest.fn(),
	})),
});

// Mock scrollTo
window.scrollTo = jest.fn();

// Mock history APIs missing in JSDOM
if (!window.history.pushState) {
	(window.history as any).pushState = jest.fn();
}
if (!window.history.replaceState) {
	(window.history as any).replaceState = jest.fn();
}
