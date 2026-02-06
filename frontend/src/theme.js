// Create a theme file: frontend/src/theme.js
export const hydrosparkTheme = {
  colors: {
    primary: '#0A4C78',      // Deep Aqua Blue
    secondary: '#1EA7D6',    // Spark Blue
    background: '#E6F6FB',   // Soft Sky Blue
    surface: '#FFFFFF',      // Clean White
    text: '#2E2E2E',         // Charcoal Gray
    success: '#5FB58C',      // Fresh Green
  }
};

// Use in components:
import { hydrosparkTheme } from './theme';

<Button 
  style={{ 
    backgroundColor: hydrosparkTheme.colors.primary,
    color: 'white'
  }}
>
  Generate Bills
</Button>