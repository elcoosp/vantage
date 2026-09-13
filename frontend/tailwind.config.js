/** @type {import('tailwindcss').Config} */
export default {
	darkMode: "class",
	content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
	theme: {
		extend: {
			fontFamily: {
				sans: [
					'"Inter Variable"',
					"-apple-system",
					"BlinkMacSystemFont",
					'"Segoe UI"',
					"ui-sans-serif",
					"system-ui",
					"sans-serif",
				],
				mono: ['"JetBrains Mono Variable"', "ui-monospace", "SFMono-Regular", "Menlo", "monospace"],
			},
			colors: {
				// Neutral canvas + surfaces (light/dark pairs)
				canvas: { light: "#F7F8FA", dark: "#0B0C10" },
				card: { light: "#FFFFFF", dark: "#15171D" },
				card2: { light: "#F3F4F8", dark: "#1D2029" },
				ink: { light: "#17181C", dark: "#F4F5F7" },
				ink2: { light: "#5A5E66", dark: "#A6ACB6" },
				ink3: { light: "#878C96", dark: "#6C7380" },
				line: { light: "#E7E9EF", dark: "#262A33" },
				line2: { light: "#D8DBE2", dark: "#353B47" },

				// Brand — a calm violet-indigo
				brand: {
					50: "#F0F1FE",
					100: "#E1E3FD",
					200: "#CACDFB",
					300: "#A8ADF6",
					400: "#8389EA",
					500: "#646BDE",
					600: "#5257CE",
					700: "#4347AA",
					800: "#36398A",
					900: "#2E306B",
				},

				// Status semantics (tinted soft + solid + ink)
				status: {
					success: {
						soft: "#E9F6EE",
						solid: "#1EAD72",
						ink: "#127A52",
					},
					warning: {
						soft: "#FCF3E2",
						solid: "#D3932B",
						ink: "#8A5E0B",
					},
					danger: {
						soft: "#FDEBEC",
						solid: "#E5484D",
						ink: "#B23B3F",
					},
					info: {
						soft: "#ECEFFD",
						solid: "#5B7CFA",
						ink: "#3B5BD9",
					},
				},
			},
			boxShadow: {
				card: "0 1px 2px 0 rgba(23, 24, 28, 0.03), 0 1px 3px 0 rgba(23, 24, 28, 0.04)",
				lift: "0 4px 12px -2px rgba(23, 24, 28, 0.1), 0 2px 6px -2px rgba(23, 24, 28, 0.06)",
				pop: "0 12px 32px -8px rgba(23, 24, 28, 0.18), 0 4px 12px -4px rgba(23, 24, 28, 0.08)",
				float: "0 24px 56px -16px rgba(23, 24, 28, 0.28), 0 8px 20px -8px rgba(23, 24, 28, 0.12)",
			},
			keyframes: {
				"fade-up": {
					"0%": { opacity: "0", transform: "translateY(4px)" },
					"100%": { opacity: "1", transform: "translateY(0)" },
				},
				"scale-in": {
					"0%": { opacity: "0", transform: "scale(0.97) translateY(4px)" },
					"100%": { opacity: "1", transform: "scale(1) translateY(0)" },
				},
			},
			animation: {
				"fade-up": "fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) both",
				"scale-in": "scale-in 0.2s cubic-bezier(0.16, 1, 0.3, 1) both",
			},
		},
	},
	plugins: [],
};
