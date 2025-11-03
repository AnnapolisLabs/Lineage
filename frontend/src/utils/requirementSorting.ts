/**
 * Utility functions for sorting requirements by their IDs
 */

/**
 * Compare two requirement IDs using natural number ordering
 */
export function compareReqIds(reqId1: string, reqId2: string): number {
  try {
    // Extract numeric portion after the last dash
    const num1 = extractNumber(reqId1)
    const num2 = extractNumber(reqId2)

    // If numbers are different, sort by number
    if (num1 !== num2) {
      return num1 - num2
    }

    // If numbers are same, sort by full string (handles different prefixes)
    return reqId1.localeCompare(reqId2)
  } catch (e: any) {
    // Fallback to string comparison if parsing fails
    console.debug('Error comparing requirement IDs:', e.message)
    return reqId1.localeCompare(reqId2)
  }
}

/**
 * Extract the numeric portion from a requirement ID
 */
export function extractNumber(reqId: string): number {
  // Find the last dash and extract the number after it
  const lastDash = reqId.lastIndexOf('-')
  if (lastDash >= 0 && lastDash < reqId.length - 1) {
    const numPart = reqId.substring(lastDash + 1)
    return Number.parseInt(numPart, 10)
  }
  return 0
}
