package com.wedding.wedding_management_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wedding.wedding_management_system.dto.TaskDTO;
import com.wedding.wedding_management_system.entity.Document;
import com.wedding.wedding_management_system.entity.ProjectTask;

import com.wedding.wedding_management_system.repository.DocumentRepository;
import com.wedding.wedding_management_system.repository.ProjectTaskRepository;

@ExtendWith(MockitoExtension.class)
public class ProjectTaskServiceTest {

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private ProjectTaskService projectTaskService;

    @Test
    @DisplayName("測試根據員工 ID 獲取歷史任務 - 應回傳待審核與已完成的任務")
    public void getHistoryTasksByEmployeeId_empId1_shouldReturnPendingAndCompletedTasks() {
        Integer empId = 1;
        List<String> statuses = List.of("待審核", "已完成");

        List<TaskDTO> mockTasks = List.of(
                new TaskDTO(Integer.valueOf(101), "已完成", "測試任務1", null, null, null, null, null, null),
                new TaskDTO(Integer.valueOf(102), "待審核", "測試任務2", null, null, null, null, null, null));

        when(projectTaskRepository.findTasksByEmployeeIdAndStatuses(empId, statuses))
                .thenReturn(mockTasks);

        List<TaskDTO> result = projectTaskService.getHistoryTasksByEmployeeId(empId);

        assertEquals(2, result.size());
        assertEquals("測試任務1", result.get(0).getServiceName());
        assertEquals("已完成", result.get(0).getStatus());
        assertEquals("測試任務2", result.get(1).getServiceName());
        assertEquals("待審核", result.get(1).getStatus());

        verify(projectTaskRepository, times(1)).findTasksByEmployeeIdAndStatuses(empId, statuses);
    }

    @Test
    @DisplayName("測試根據員工ID獲取歷史任務 - 無任務情況")
    public void getHistoryTasksByEmployeeId_noTask_shouldReturnEmptyList() {
        Integer empId = 1;
        List<String> statuses = List.of("待審核", "已完成");

        List<TaskDTO> mockTasks = List.of();

        when(projectTaskRepository.findTasksByEmployeeIdAndStatuses(empId, statuses))
                .thenReturn(mockTasks);

        List<TaskDTO> result = projectTaskService.getHistoryTasksByEmployeeId(empId);

        assertEquals(0, result.size());

        verify(projectTaskRepository, times(1)).findTasksByEmployeeIdAndStatuses(empId, statuses);
    }

    @Test
    @DisplayName("測試獲取任務後獲取該任務的附檔")
    public void populateDocuments_haveDocument_shouldReturnDocumentList() {
        Integer empId = 1;
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTaskId(101);

        when(projectTaskRepository.findTasksByEmployeeIdAndStatuses(anyInt(), anyList()))
                .thenReturn(List.of(taskDTO));

        Document doc = new Document();
        doc.setId(501);
        doc.setName("測試檔案1");

        // !!! 重要：這裡要模擬 Document 關聯到 Task，因為程式碼有 d.getTask().getId()
        ProjectTask taskEntity = new ProjectTask();
        taskEntity.setId(101);
        doc.setTask(taskEntity);

        when(documentRepository.findByTask_IdIn(anyList())).thenReturn(List.of(doc));

        List<TaskDTO> result = projectTaskService.getHistoryTasksByEmployeeId(empId);
        assertEquals(1, result.size());
        List<TaskDTO.DocumentDTO> docs = result.get(0).getDocuments();

        assertNotNull(docs, "附檔不應該為 null");
        assertEquals(1, docs.size(), "附檔數量應該為 1");
        assertEquals("測試檔案1", docs.get(0).getName(), "附檔名稱應該為測試檔案1");

        verify(documentRepository, times(1)).findByTask_IdIn(anyList());
    }
}
