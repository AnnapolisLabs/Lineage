import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HelloWorld from './HelloWorld.vue'

describe('HelloWorld.vue', () => {
  it('should render component', () => {
    const wrapper = mount(HelloWorld, {
      props: {
        msg: 'Hello Test'
      }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.text()).toContain('Hello Test')
  })

  it('should display the message prop', () => {
    const testMessage = 'Test Message'
    const wrapper = mount(HelloWorld, {
      props: {
        msg: testMessage
      }
    })

    expect(wrapper.text()).toContain(testMessage)
  })
})
