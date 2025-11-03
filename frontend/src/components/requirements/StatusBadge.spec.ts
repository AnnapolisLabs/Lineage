import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from './StatusBadge.vue'

describe('StatusBadge.vue', () => {
  it('should render status badge', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'APPROVED' }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.text()).toContain('APPROVED')
  })

  it('should apply correct classes for APPROVED status', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'APPROVED' }
    })

    const badge = wrapper.find('[class*="bg-green"]')
    expect(badge.exists()).toBe(true)
  })

  it('should apply correct classes for REVIEW status', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'REVIEW' }
    })

    const badge = wrapper.find('[class*="bg-yellow"]')
    expect(badge.exists()).toBe(true)
  })

  it('should apply correct classes for DEPRECATED status', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'DEPRECATED' }
    })

    const badge = wrapper.find('[class*="bg-red"]')
    expect(badge.exists()).toBe(true)
  })
})
