module.exports = {
	root: true,
	env: {
		browser: true,
		es2022: true,
		jest: true,
	},
	parser: '@typescript-eslint/parser',
	parserOptions: {
		ecmaVersion: 'latest',
		sourceType: 'module',
		ecmaFeatures: { jsx: true },
		project: undefined,
	},
	settings: {
		react: { version: 'detect' },
		'import/resolver': {
			typescript: {
				project: ['./tsconfig.json'],
				alwaysTryTypes: true,
			},
		},
	},
	plugins: ['react', 'react-hooks', '@typescript-eslint', 'import', 'unused-imports', 'prettier'],
	extends: [
		'eslint:recommended',
		'plugin:react/recommended',
		'plugin:react-hooks/recommended',
		'plugin:@typescript-eslint/recommended',
		'plugin:import/recommended',
		'plugin:import/typescript',
		'plugin:prettier/recommended',
	],
	rules: {
		'prettier/prettier': ['error'],
		'react/react-in-jsx-scope': 'off', // React 17+
		'@typescript-eslint/no-unused-vars': 'off', // handled by unused-imports
		'unused-imports/no-unused-imports': 'error',
		'unused-imports/no-unused-vars': [
			'warn',
			{ args: 'after-used', argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
		],
		'import/order': [
			'warn',
			{
				groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index', 'object', 'type'],
				'newlines-between': 'always',
				alphabetize: { order: 'asc', caseInsensitive: true },
			},
		],
		'react/prop-types': 'off',
		'@typescript-eslint/explicit-function-return-type': 'off',
		'@typescript-eslint/no-explicit-any': 'warn',
	},
	overrides: [
		{
			files: ['*.test.*', 'src/setupTests.ts'],
			rules: {
				'@typescript-eslint/no-explicit-any': 'off',
				'import/no-named-as-default': 'off',
				'import/no-named-as-default-member': 'off',
			},
		},
	],
};
