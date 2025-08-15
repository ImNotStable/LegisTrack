import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { rest } from 'msw';
import { DocumentDetail } from '../DocumentDetail';
import { server } from '../../mocks/handlers';
import { ToastProvider } from '../Toast';

// Mock navigate at module level
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Setup MSW
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const renderWithProviders = (documentId: string = '1') => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastProvider>
          <Routes>
            <Route path="/document/:id" element={<DocumentDetail />} />
          </Routes>
        </ToastProvider>
      </BrowserRouter>
    </QueryClientProvider>,
    { wrapper: ({ children }) => <div>{children}</div> }
  );
};

describe('DocumentDetail', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Ensure history APIs exist in JSDOM
    if (!window.history.pushState) {
      // @ts-ignore
      window.history.pushState = jest.fn();
    }
    if (!window.history.replaceState) {
      // @ts-ignore
      window.history.replaceState = jest.fn();
    }
  });

  it('renders loading spinner initially', () => {
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('displays document details after loading', async () => {
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      expect(screen.getByText('Test Bill for Healthcare Reform')).toBeInTheDocument();
      expect(screen.getByText('H.R. H.R.1234')).toBeInTheDocument();
      expect(screen.getByText('This bill addresses healthcare reform and insurance coverage.')).toBeInTheDocument();
    });
  });

  it('shows sponsors information correctly', async () => {
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      expect(screen.getByText('Sponsors (37)')).toBeInTheDocument();
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Primary')).toBeInTheDocument();
      expect(screen.getByText('Democratic - CA (District 12)')).toBeInTheDocument();
    });
  });

  it('displays AI analysis when available', async () => {
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      expect(screen.getByText('AI Analysis')).toBeInTheDocument();
      expect(screen.getByText('This bill would significantly impact healthcare coverage.')).toBeInTheDocument();
      expect(screen.getByText('Expected to reduce healthcare costs by 15%.')).toBeInTheDocument();
    });
  });

  it('shows legislative actions timeline', async () => {
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      expect(screen.getByText('Legislative Actions')).toBeInTheDocument();
      expect(screen.getByText('Introduced in House')).toBeInTheDocument();
      expect(screen.getByText('Chamber: House')).toBeInTheDocument();
    });
  });

  it('handles refresh button click', async () => {
    const user = userEvent.setup();
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      expect(screen.getByText('Test Bill for Healthcare Reform')).toBeInTheDocument();
    });

    // delay refresh endpoint to observe pending state
    server.use(
      rest.post('/api/documents/1/refresh', (req, res, ctx) => {
        return res(ctx.delay(150), ctx.status(200), ctx.json({ success: true }));
      })
    );

    const refreshButton = screen.getByText('Refresh');
    await user.click(refreshButton);

    // Button should be disabled while loading
    await waitFor(() => expect(refreshButton).toBeDisabled());
  });

  it('handles analyze button click', async () => {
    const user = userEvent.setup();

    // Mock document without analysis
    server.use(
      rest.get('/api/documents/1', (req, res, ctx) => {
        return res(
          ctx.status(200),
          ctx.json({
            id: 1,
            billId: 'H.R.1234',
            title: 'Test Bill for Healthcare Reform',
            officialSummary: 'This bill addresses healthcare reform and insurance coverage.',
            introductionDate: '2024-01-15',
            congressSession: 118,
            billType: 'H.R.',
            fullTextUrl: 'https://congress.gov/bill/test',
            status: 'introduced',
            sponsors: [],
            actions: [],
            analysis: null,
            partyBreakdown: {
              democratic: 25,
              republican: 10,
              independent: 2,
              other: 0,
              total: 37,
              democraticPercentage: 67.6,
              republicanPercentage: 27.0,
            },
            createdAt: '2024-01-15T10:00:00Z',
            updatedAt: '2024-01-16T12:00:00Z',
          })
        );
      }),
      rest.post('/api/documents/1/analyze', (req, res, ctx) => {
        return res(ctx.delay(150), ctx.status(200), ctx.json({ success: true }));
      })
    );

    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      expect(screen.getByText('Analyze with AI')).toBeInTheDocument();
    });

    const analyzeButton = await screen.findByText('Analyze with AI');
    await user.click(analyzeButton);
    // allow mutation state to propagate
    await waitFor(() => expect(analyzeButton).toBeDisabled());
  });

  it('displays error message for non-existent document', async () => {
    server.use(
      rest.get('/api/documents/999', (req, res, ctx) => {
        return res(ctx.status(404), ctx.json({ message: 'Document not found' }));
      })
    );

    window.history.pushState({}, '', '/document/999');
    renderWithProviders('999');

    expect(await screen.findByText('Error loading document')).toBeInTheDocument();
  });

  it('handles back navigation', async () => {
    const user = userEvent.setup();
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      expect(screen.getByText('Back to Documents')).toBeInTheDocument();
    });

    const backButton = await screen.findByText('Back to Documents');
    await user.click(backButton);

    // Navigation should be called
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it('renders external link correctly', async () => {
    window.history.pushState({}, '', '/document/1');
    renderWithProviders('1');

    await waitFor(() => {
      const externalLink = screen.getByText('View Full Text on Congress.gov');
      expect(externalLink.closest('a')).toHaveAttribute('href', 'https://congress.gov/bill/test');
      expect(externalLink.closest('a')).toHaveAttribute('target', '_blank');
    });
  });
});
