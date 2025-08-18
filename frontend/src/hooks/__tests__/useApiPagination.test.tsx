import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useDocuments } from '../useApi';
import { apiService } from '../../services/api';

jest.mock('../../services/api');

(apiService.getDocuments as jest.Mock).mockResolvedValue({
  content: [],
  number: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  numberOfElements: 0,
  first: true,
  last: true,
});

const createWrapper = () => {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
};

describe('Pagination sanitization', () => {
  beforeEach(() => {
    (apiService.getDocuments as jest.Mock).mockClear();
  });

  it('invokes API even if NaN page supplied (service clamps internally)', async () => {
    const { result } = renderHook(() => useDocuments(Number.NaN as any, 20, 'introductionDate', 'desc'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess || result.current.isError).toBeTruthy());
    expect(apiService.getDocuments).toHaveBeenCalledTimes(1);
  });

  it('invokes API with negative page (service clamps internally)', async () => {
    const { result } = renderHook(() => useDocuments(-5, 20, 'introductionDate', 'desc'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess || result.current.isError).toBeTruthy());
    expect(apiService.getDocuments).toHaveBeenCalledTimes(1);
  });

});
