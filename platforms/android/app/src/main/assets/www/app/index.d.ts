type K = keyof GlobalEventHandlers;

interface EvtHandlers {
	[key: K]: GlobalEventHandlers[K];
}

type CKey = keyof CSSStyleDeclaration;

interface CssHandlers {
	[key: string]: CSSStyleDeclaration[CKey];
}

declare interface options {
	class?: string[];
	id?: string;
	innerHTML?: string;
	innerText?: string;
	parentNode?: HTMLElement | HTMLDivElement;
	listen?: EvtHandlers;
	style?: CssHandlers;
}

declare interface Window {
	JSZip3: any;
}