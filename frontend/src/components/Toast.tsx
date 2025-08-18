import * as React from 'react';

type ToastVariant = 'success' | 'error' | 'info' | 'warning';

export type Toast = {
	id: string;
	title?: string;
	message: string;
	variant?: ToastVariant;
	durationMs?: number;
};

type ToastContextValue = {
	show: (toast: Omit<Toast, 'id'>) => string;
	success: (message: string, opts?: Partial<Omit<Toast, 'id' | 'message' | 'variant'>>) => string;
	error: (message: string, opts?: Partial<Omit<Toast, 'id' | 'message' | 'variant'>>) => string;
	info: (message: string, opts?: Partial<Omit<Toast, 'id' | 'message' | 'variant'>>) => string;
	warning: (message: string, opts?: Partial<Omit<Toast, 'id' | 'message' | 'variant'>>) => string;
	dismiss: (id: string) => void;
};

const ToastContext = React.createContext<ToastContextValue | undefined>(undefined);

export const useToast = (): ToastContextValue => {
	const ctx = React.useContext(ToastContext);
	if (!ctx) throw new Error('useToast must be used within ToastProvider');
	return ctx;
};

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
	const [toasts, setToasts] = React.useState<Toast[]>([]);
	const timeoutsRef = React.useRef<Record<string, number>>({});

	const dismiss = React.useCallback((id: string) => {
		setToasts((prev) => prev.filter((t) => t.id !== id));
		const timeoutId = timeoutsRef.current[id];
		if (timeoutId) {
			window.clearTimeout(timeoutId);
			delete timeoutsRef.current[id];
		}
	}, []);

	const enqueue = React.useCallback(
		(toast: Omit<Toast, 'id'>) => {
			const id =
				typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
					? crypto.randomUUID()
					: `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
			const item: Toast = { id, durationMs: 4000, variant: 'info', ...toast };
			setToasts((prev) => [...prev, item]);
			if (item.durationMs && item.durationMs > 0) {
				timeoutsRef.current[id] = window.setTimeout(() => dismiss(id), item.durationMs);
			}
			return id;
		},
		[dismiss]
	);

	const api = React.useMemo<ToastContextValue>(
		() => ({
			show: enqueue,
			success: (message, opts) => enqueue({ message, variant: 'success', ...opts }),
			error: (message, opts) => enqueue({ message, variant: 'error', ...opts }),
			info: (message, opts) => enqueue({ message, variant: 'info', ...opts }),
			warning: (message, opts) => enqueue({ message, variant: 'warning', ...opts }),
			dismiss,
		}),
		[enqueue, dismiss]
	);

	return (
		<ToastContext.Provider value={api}>
			{children}
			<div className="fixed z-50 inset-0 pointer-events-none flex flex-col items-end gap-2 p-4 sm:p-6">
				{toasts.map((t) => (
					<ToastItem key={t.id} toast={t} onClose={() => dismiss(t.id)} />
				))}
			</div>
		</ToastContext.Provider>
	);
};

const variantClasses: Record<ToastVariant, string> = {
	success: 'bg-green-600 text-white',
	error: 'bg-red-600 text-white',
	info: 'bg-blue-600 text-white',
	warning: 'bg-yellow-500 text-gray-900',
};

const ToastItem: React.FC<{ toast: Toast; onClose: () => void }> = ({ toast, onClose }) => {
	const color = variantClasses[toast.variant ?? 'info'];
	return (
		<div className={`pointer-events-auto w-full sm:w-96 shadow-lg rounded-lg ${color}`}>
			<div className="p-4">
				{toast.title && <p className="font-semibold mb-1">{toast.title}</p>}
				<div className="flex items-start">
					<div className="flex-1 text-sm">{toast.message}</div>
					<button
						onClick={onClose}
						className="ml-3 rounded-md/50 p-1.5 hover:opacity-90 focus:outline-none"
						aria-label="Close"
					>
						×
					</button>
				</div>
			</div>
		</div>
	);
};
