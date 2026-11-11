package com.raj.codex
import com.raj.codex.agents.TaskState
import com.raj.codex.agents.TaskTransitions
import org.junit.Assert.*
import org.junit.Test
class TaskStateTest {
    @Test fun legalTransition() { assertTrue(TaskTransitions.can(TaskState.RECEIVED, TaskState.UNDERSTANDING)) }
    @Test fun illegalTransition() { assertFalse(TaskTransitions.can(TaskState.RECEIVED, TaskState.VERIFIED)) }
    @Test fun reviewToVerifiedLegal() { assertTrue(TaskTransitions.can(TaskState.REVIEWING, TaskState.VERIFIED)) }
    @Test fun testingToDebuggingLegal() { assertTrue(TaskTransitions.can(TaskState.TESTING, TaskState.DEBUGGING)) }
}
