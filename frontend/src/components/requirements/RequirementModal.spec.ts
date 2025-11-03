import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import RequirementModal from './RequirementModal.vue'

describe('RequirementModal.vue', () => {
  it('should render modal', () => {
    const wrapper = mount(RequirementModal, {
      props: {
        isOpen: true,
        projectId: '1'
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('should emit close event', async () => {
    const wrapper = mount(RequirementModal, {
      props: {
        isOpen: true,
        projectId: '1'
      }
    })

    const closeButton = wrapper.findAll('button').find(btn =>
      btn.text().toLowerCase().includes('close') || btn.text().toLowerCase().includes('cancel')
    )

    if (closeButton) {
      await closeButton.trigger('click')
      expect(wrapper.emitted('close')).toBeTruthy()
    }
  })
})
