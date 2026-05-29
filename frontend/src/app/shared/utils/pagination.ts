export type PaginationItem = number | '...';

export function buildPagination(
  currentPage: number,
  totalPages: number,
  maxVisible = 7,
): PaginationItem[] {
  const safeTotal = Math.max(1, totalPages || 1);
  const safeCurrent = Math.min(Math.max(currentPage || 1, 1), safeTotal);
  const safeVisible = Math.max(5, maxVisible);

  if (safeTotal <= safeVisible) {
    return Array.from({ length: safeTotal }, (_, index) => index + 1);
  }

  const windowSize = safeVisible - 2;
  let start = Math.max(2, safeCurrent - Math.floor(windowSize / 2));
  let end = Math.min(safeTotal - 1, start + windowSize - 1);
  start = Math.max(2, end - windowSize + 1);

  const items: PaginationItem[] = [1];

  if (start > 2) {
    items.push('...');
  }

  for (let page = start; page <= end; page += 1) {
    items.push(page);
  }

  if (end < safeTotal - 1) {
    items.push('...');
  }

  items.push(safeTotal);
  return items;
}
