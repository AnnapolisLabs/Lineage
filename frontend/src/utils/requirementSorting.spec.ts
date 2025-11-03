import { describe, it, expect } from 'vitest'
import { compareReqIds, extractNumber } from './requirementSorting'

describe('requirementSorting', () => {
  describe('extractNumber', () => {
    it('should extract number after last dash', () => {
      expect(extractNumber('REQ-1')).toBe(1)
      expect(extractNumber('REQ-100')).toBe(100)
      expect(extractNumber('PRJ-REQ-42')).toBe(42)
    })

    it('should return 0 when no number found', () => {
      expect(extractNumber('REQ')).toBe(0)
      expect(isNaN(extractNumber('REQ-'))).toBe(true)
      expect(extractNumber('NODASH')).toBe(0)
    })

    it('should handle empty string', () => {
      expect(extractNumber('')).toBe(0)
    })
  })

  describe('compareReqIds', () => {
    it('should sort by number when prefixes are same', () => {
      expect(compareReqIds('REQ-1', 'REQ-2')).toBeLessThan(0)
      expect(compareReqIds('REQ-10', 'REQ-2')).toBeGreaterThan(0)
      expect(compareReqIds('REQ-100', 'REQ-20')).toBeGreaterThan(0)
    })

    it('should return 0 for identical IDs', () => {
      expect(compareReqIds('REQ-1', 'REQ-1')).toBe(0)
    })

    it('should sort by string when numbers are equal', () => {
      expect(compareReqIds('AAA-1', 'BBB-1')).toBeLessThan(0)
      expect(compareReqIds('BBB-1', 'AAA-1')).toBeGreaterThan(0)
    })

    it('should handle mixed formats gracefully', () => {
      expect(compareReqIds('REQ-1', 'NO-DASH')).not.toBe(0)
      expect(compareReqIds('NO-DASH', 'REQ-1')).not.toBe(0)
    })

    it('should sort naturally by number', () => {
      const ids = ['REQ-10', 'REQ-2', 'REQ-1', 'REQ-20', 'REQ-3']
      const sorted = ids.sort(compareReqIds)
      expect(sorted).toEqual(['REQ-1', 'REQ-2', 'REQ-3', 'REQ-10', 'REQ-20'])
    })
  })
})
