import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PriorityBadge from './PriorityBadge.vue'

describe('PriorityBadge.vue', () => {
  it('should render priority badge', () => {
    const wrapper = mount(PriorityBadge, {
      props: { priority: 'HIGH' }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.text()).toContain('HIGH')
  })

  it('should apply correct classes for CRITICAL priority', () => {
    const wrapper = mount(PriorityBadge, {
      props: { priority: 'CRITICAL' }
    })

    const badge = wrapper.find('[class*="bg-red"]')
    expect(badge.exists()).toBe(true)
  })

  it('should apply correct classes for HIGH priority', () => {
    const wrapper = mount(PriorityBadge, {
      props: { priority: 'HIGH' }
    })

    const badge = wrapper.find('[class*="bg-orange"]')
    expect(badge.exists()).toBe(true)
  })

  it('should apply correct classes for MEDIUM priority', () => {
    const wrapper = mount(PriorityBadge, {
      props: { priority: 'MEDIUM' }
    })

    const badge = wrapper.find('[class*="bg-annapolis-teal"]')
    expect(badge.exists()).toBe(true)
  })
})
