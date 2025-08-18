import React from 'react';

// This component exists purely to ensure Tailwind includes all gradient classes
// It's not rendered anywhere but ensures the CSS classes are in the final build
export const TailwindTest = () => {
	return (
		<div className="hidden">
			{/* ORIGINAL STRONG GRADIENTS - ALL CLASSES SEPARATED */}
			<div className="bg-gradient-to-br from-red-400 via-red-300 to-red-200">Republican High</div>
			<div className="from-red-400">Red Start</div>
			<div className="via-red-300">Red Middle</div>
			<div className="to-red-200">Red End</div>

			<div className="bg-gradient-to-br from-blue-400 via-blue-300 to-blue-200">
				Democratic High
			</div>
			<div className="from-blue-400">Blue Start</div>
			<div className="via-blue-300">Blue Middle</div>
			<div className="to-blue-200">Blue End</div>

			<div className="bg-gradient-to-br from-purple-400 via-purple-300 to-purple-200">
				Mixed Support
			</div>
			<div className="from-purple-400">Purple Start</div>
			<div className="via-purple-300">Purple Middle</div>
			<div className="to-purple-200">Purple End</div>

			{/* NEW SUBTLE GRADIENTS - SIMPLIFIED */}
			<div className="bg-gradient-to-br from-red-100 to-white">Subtle Republican</div>
			<div className="from-red-100">Subtle Red Start</div>

			<div className="bg-gradient-to-br from-blue-100 to-white">Subtle Democratic</div>
			<div className="from-blue-100">Subtle Blue Start</div>

			<div className="bg-gradient-to-br from-purple-100 to-white">Subtle Mixed</div>
			<div className="from-purple-100">Subtle Purple Start</div>
			<div className="to-white">White End</div>

			{/* Base gradient direction */}
			<div className="bg-gradient-to-br">Base Gradient</div>
		</div>
	);
};
