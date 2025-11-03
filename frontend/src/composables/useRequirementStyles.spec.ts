import { describe, it, expect } from 'vitest'
import { useRequirementStyles } from './useRequirementStyles'

describe('useRequirementStyles', () => {
  const { getStatusClasses, getPriorityClasses } = useRequirementStyles()

  describe('getStatusClasses', () => {
    it('should return approved classes for APPROVED status', () => {
      const classes = getStatusClasses('APPROVED')
      expect(classes).toContain('bg-green-500/20')
      expect(classes).toContain('text-green-400')
      expect(classes).toContain('border-green-500/30')
    })

    it('should return review classes for REVIEW status', () => {
      const classes = getStatusClasses('REVIEW')
      expect(classes).toContain('bg-yellow-500/20')
      expect(classes).toContain('text-yellow-400')
      expect(classes).toContain('border-yellow-500/30')
    })

    it('should return deprecated classes for DEPRECATED status', () => {
      const classes = getStatusClasses('DEPRECATED')
      expect(classes).toContain('bg-red-500/20')
      expect(classes).toContain('text-red-400')
      expect(classes).toContain('border-red-500/30')
    })

    it('should return default classes for unknown status', () => {
      const classes = getStatusClasses('UNKNOWN')
      expect(classes).toContain('bg-annapolis-gray-600/20')
      expect(classes).toContain('text-annapolis-gray-300')
    })

    it('should include base classes for all statuses', () => {
      const classes = getStatusClasses('APPROVED')
      expect(classes).toContain('inline-flex')
      expect(classes).toContain('items-center')
      expect(classes).toContain('rounded-full')
    })
  })

  describe('getPriorityClasses', () => {
    it('should return critical classes for CRITICAL priority', () => {
      const classes = getPriorityClasses('CRITICAL')
      expect(classes).toContain('bg-red-500/20')
      expect(classes).toContain('text-red-400')
      expect(classes).toContain('border-red-500/30')
    })

    it('should return high classes for HIGH priority', () => {
      const classes = getPriorityClasses('HIGH')
      expect(classes).toContain('bg-orange-500/20')
      expect(classes).toContain('text-orange-400')
      expect(classes).toContain('border-orange-500/30')
    })

    it('should return medium classes for MEDIUM priority', () => {
      const classes = getPriorityClasses('MEDIUM')
      expect(classes).toContain('bg-annapolis-teal/20')
      expect(classes).toContain('text-annapolis-teal')
    })

    it('should return default classes for unknown priority', () => {
      const classes = getPriorityClasses('UNKNOWN')
      expect(classes).toContain('bg-annapolis-gray-600/20')
      expect(classes).toContain('text-annapolis-gray-300')
    })

    it('should include base classes for all priorities', () => {
      const classes = getPriorityClasses('HIGH')
      expect(classes).toContain('inline-flex')
      expect(classes).toContain('items-center')
      expect(classes).toContain('rounded-full')
    })
  })
})
