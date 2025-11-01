export function useRequirementStyles() {
  const getStatusClasses = (status: string) => {
    const baseClasses = 'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border'

    switch (status) {
      case 'APPROVED':
        return `${baseClasses} bg-green-500/20 text-green-400 border-green-500/30`
      case 'REVIEW':
        return `${baseClasses} bg-yellow-500/20 text-yellow-400 border-yellow-500/30`
      case 'DEPRECATED':
        return `${baseClasses} bg-red-500/20 text-red-400 border-red-500/30`
      default:
        return `${baseClasses} bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30`
    }
  }

  const getPriorityClasses = (priority: string) => {
    const baseClasses = 'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border'

    switch (priority) {
      case 'CRITICAL':
        return `${baseClasses} bg-red-500/20 text-red-400 border-red-500/30`
      case 'HIGH':
        return `${baseClasses} bg-orange-500/20 text-orange-400 border-orange-500/30`
      case 'MEDIUM':
        return `${baseClasses} bg-annapolis-teal/20 text-annapolis-teal border-annapolis-teal/30`
      default:
        return `${baseClasses} bg-annapolis-gray-600/20 text-annapolis-gray-300 border-annapolis-gray-600/30`
    }
  }

  return {
    getStatusClasses,
    getPriorityClasses
  }
}
