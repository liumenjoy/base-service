package io.choerodon.base.api.controller.v1

import com.github.pagehelper.PageInfo
import io.choerodon.base.IntegrationTestConfiguration
import io.choerodon.base.api.dto.RoleAssignmentDeleteDTO
import io.choerodon.base.api.dto.RoleAssignmentSearchDTO
import io.choerodon.base.api.dto.UploadHistoryDTO
import io.choerodon.base.api.query.ClientRoleQuery
import io.choerodon.base.app.service.RoleService
import io.choerodon.base.app.service.UserService
import org.springframework.data.domain.PageRequest
import io.choerodon.base.infra.dto.*
import io.choerodon.base.infra.enums.MemberType
import io.choerodon.base.infra.mapper.*
import io.choerodon.core.exception.ExceptionResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

/**
 * @author dengyouquan
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Stepwise
class RoleMemberControllerSpec extends Specification {
    private static final String BASE_PATH = "/v1"
    @Autowired
    private TestRestTemplate restTemplate

    @Autowired
    private UserService userService

    @Autowired
    private RoleService roleService

    @Autowired
    private MemberRoleMapper memberRoleMapper
    @Autowired
    private RoleMapper roleMapper
    @Autowired
    private UserMapper userMapper
    @Autowired
    private ProjectMapper projectMapper
    @Autowired
    private ClientMapper clientMapper
    @Shared
    def needInit = true
    @Shared
    def needClean = false
    @Shared
    def memberRoleDOList = new ArrayList<MemberRoleDTO>()
    @Shared
    def roleDOList = new ArrayList<RoleDTO>()
    @Shared
    def userDOList = new ArrayList<UserDTO>()
    @Shared
    def clientDOList
    @Shared
    def projectDO = new ProjectDTO()


    def setup() {
        if (needInit) {
            given: "构造参数"
            needInit = false
            for (int i = 0; i < 3; i++) {
                RoleDTO roleDO = new RoleDTO()
                roleDO.setCode("role/site/default/rolemember" + i)
                roleDO.setName("权限管理员")
                roleDO.setResourceLevel("site")
                roleDOList.add(roleDO)
            }
            projectDO.setCode("hand")
            projectDO.setName("汉得")
            projectDO.setOrganizationId(1L)

            when: "插入记录"
            def count = 0
            count += projectMapper.insert(projectDO)
            for (RoleDTO roleDO : roleDOList) {
                count += roleMapper.insert(roleDO)
            }
            for (int i = 0; i < 3; i++) {
                UserDTO user = new UserDTO()
                user.setLoginName("dengyouquan" + i)
                user.setRealName("dengyouquan" + i)
                user.setEmail("dengyouquan" + i + "@qq.com")
                user.setSourceId(projectDO.getId())
                user.setOrganizationId(1L)
                userDOList.add(user)
            }
            for (UserDTO dto : userDOList) {
                count++
                userMapper.insert(dto)
            }
            for (int i = 0; i < 3; i++) {
                MemberRoleDTO memberRoleDO = new MemberRoleDTO()
                memberRoleDO.setMemberType("user")
                memberRoleDO.setRoleId(roleDOList.get(i).getId())
                memberRoleDO.setSourceType("site")
                memberRoleDO.setSourceId(0)
                memberRoleDO.setMemberId()
                memberRoleDOList.add(memberRoleDO)
            }
            for (MemberRoleDTO dto : memberRoleDOList) {
                count++
                memberRoleMapper.insert(dto)

            }
            clientDOList = initClient()

            then: "校验结果"
            count == 10
        }
    }

    def cleanup() {
        if (needClean) {
            given: ""
            def count = 0
            needClean = false

            when: "删除记录"
            for (MemberRoleDTO memberRoleDO : memberRoleDOList) {
                count += memberRoleMapper.deleteByPrimaryKey(memberRoleDO)
            }
            for (UserDTO userDO : userDOList) {
                count += userMapper.deleteByPrimaryKey(userDO)
            }
            for (RoleDTO roleDO : roleDOList) {
                count += roleMapper.deleteByPrimaryKey(roleDO)
            }

            for (ClientDTO clientDO : clientDOList) {
                clientMapper.deleteByPrimaryKey(clientDO)
            }
            count += projectMapper.deleteByPrimaryKey(projectDO)

            then: "校验结果"
            count == 10
        }
    }

    List<ClientDTO> initClient() {
        List<ClientDTO> clientDOList = new ArrayList<>()
        for (int i = 0; i < 3; i++) {
            ClientDTO clientDO = new ClientDTO()
            clientDO.setName("client" + i)
            clientDO.setOrganizationId(1L)
            clientMapper.insertSelective(clientDO)
            clientDOList.add(clientDO)
        }

        return clientDOList
    }

    def "listRolesOnSiteLevel"() {
        given: "参数构造"
        RoleMemberController controller = new RoleMemberController(null, userService, roleService, null, null, null)
        String roleName = "平台开发者"

        when: "方法调用"
        def result = controller.listRolesOnSiteLevel(roleName)
        then: "结果校验"
        result.getBody().size() != 0
    }

    def "listRolesOnOrganizationLevel"() {
        given: "参数构造"
        RoleMemberController controller = new RoleMemberController(null, userService, roleService, null, null, null)
        String roleName = "组织成员"

        when: "方法调用"
        def result = controller.listRolesOnOrganizationLevel(1L, roleName)
        then: "结果校验"
        result.getBody().size() != 0
    }

    def "listRolesOnProjectLevel"() {
        given: "参数构造"
        RoleMemberController controller = new RoleMemberController(null, userService, roleService, null, null, null)
        String roleName = "项目成员"
        when: "方法调用"
        def result = controller.listRolesOnProjectLevel(1L, roleName)
        then: "结果校验"
        result.getBody().size() != 0
    }

    def "listUsersOnSiteLevel"() {
        given: "参数构造"
        Map<String, Object> map = new HashMap<>()
        map.put("user_name", "管理员")

        when: "方法调用"
        def result = restTemplate.getForEntity(BASE_PATH + "/site/enableUsers?user_name={user_name}", List, map)
        then: "结果校验"
        result.getBody().size() != 0
    }

    def "listUsersOnOrganizationLevel"() {
        given: "参数构造"
        Map<String, Object> map = new HashMap<>()
        map.put("organization_id", 1L)
        map.put("user_name", "管理员")

        when: "方法调用"
        def result = restTemplate.getForEntity(BASE_PATH + "/organizations/{organization_id}/enableUsers?user_name={user_name}", List, map)
        then: "结果校验"
        result.getBody().size() != 0
    }

    def "listUsersOnProjectLevel"() {
        Map<String, Object> map = new HashMap<>()
        given: "参数构造"
        map.put("project_id", 1L)
        map.put("user_name", "管理员")

        when: "方法调用"
        def result = restTemplate.getForEntity(BASE_PATH + "/projects/{project_id}/enableUsers?user_name={user_name}", List, map)
        then: "结果校验"
        result.getBody().size() != 0
    }

    def "CreateOrUpdateOnSiteLevel"() {
        given: "构造参数列表"
        def paramsMap = new HashMap<String, Object>()
        Long[] memberIds = new Long[2]
        memberIds[0] = userDOList.get(0).getId()
        memberIds[1] = userDOList.get(1).getId()
        paramsMap.put("is_edit", true)
        paramsMap.put("member_ids", memberIds)

        when: "调用方法[异常-role id为空]"
        def memberRoleDO = new MemberRoleDTO()
        def memberRoleDOList1 = new ArrayList<MemberRoleDTO>()
        memberRoleDOList1.add(memberRoleDO)
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList1, ExceptionResponse, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().getCode().equals("error.roleId.null")

        when: "调用方法[异常-role不存在]"
        memberRoleDO.setRoleId(1000L)
        entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList1, ExceptionResponse, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().getCode().equals("error.role.not.exist")

        when: "调用方法"
        entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList, List, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        !entity.getBody().isEmpty()
    }

    def "CreateOrUpdateClientRoleOnSiteLevel"() {
        given: "构造参数列表"
        def paramsMap = new HashMap<String, Object>()
        Long[] memberIds = new Long[2]
        memberIds[0] = clientDOList.get(0).getId()
        memberIds[1] = clientDOList.get(1).getId()
        paramsMap.put("is_edit", true)
        paramsMap.put("member_ids", memberIds)

        when: "调用方法[异常-role id为空]"
        def memberRoleDO = new MemberRoleDTO()
        memberRoleDO.setMemberType(MemberType.CLIENT.value())
        def memberRoleDOList1 = new ArrayList<MemberRoleDTO>()
        memberRoleDOList1.add(memberRoleDO)
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList1, ExceptionResponse, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().getCode().equals("error.roleId.null")

        when: "调用方法[异常-role不存在]"
        memberRoleDO.setRoleId(1000L)
        entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList1, ExceptionResponse, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().getCode().equals("error.role.not.exist")

        when: "调用方法"
        memberRoleDO.setRoleId(roleDOList.get(0).getId())
        memberRoleDO.setMemberType(MemberType.CLIENT.value())
        entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList, String, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody() != null
    }

    def "CreateOrUpdateOnOrganizationLevel"() {
        given: "构造参数列表"
        def paramsMap = new HashMap<String, Object>()
        Long[] memberIds = new Long[1]
        memberIds[0] = 1L
        paramsMap.put("organization_id", 1L)
        paramsMap.put("is_edit", true)
        paramsMap.put("member_ids", memberIds)
        MemberRoleDTO memberRoleDO = new MemberRoleDTO()
        memberRoleDO.setSourceType("organization")
        def memberRoleDOList1 = memberRoleMapper.select(memberRoleDO)

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList, ExceptionResponse, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().getCode().equals("error.roles.in.same.level")

//        when: "调用方法"
//        def memberIds1 = new Long[1]
//        memberIds1[0] = 1L
//        paramsMap.put("member_ids", memberIds1)
//        entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList1, ExceptionResponse, paramsMap)
//
//        then: "校验结果"
//        entity.statusCode.is2xxSuccessful()
//        entity.getBody().getCode().equals("error.roles.in.same.level")
////        !entity.getBody().isEmpty()

        when: "调用方法"
        def memberIds2 = new Long[1]
        memberIds2[0] = 1L
        paramsMap.put("member_ids", memberIds2)
        paramsMap.put("organization_id", 1L)
        memberRoleDOList1 = new ArrayList<MemberRoleDTO>()
        MemberRoleDTO memberRoleDO1 = new MemberRoleDTO()
        memberRoleDO1.setMemberId(1L)
        memberRoleDO1.setMemberType(MemberType.CLIENT.value())
        memberRoleDO1.setRoleId(2L)
        memberRoleDO1.setSourceId(1L)
        memberRoleDO1.setSourceType("organization")
        memberRoleDOList1.add(memberRoleDO1)
        entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members?is_edit={is_edit}&member_ids={member_ids}", ExceptionResponse, ExceptionResponse, paramsMap)

        then: "校验结果"
        noExceptionThrown()
    }

    def "CreateOrUpdateOnProjectLevel"() {
        given: "构造参数列表"
        def paramsMap = new HashMap<String, Object>()
        Long[] memberIds = new Long[1]
        memberIds[0] = 1L
        paramsMap.put("project_id", 1L)
        paramsMap.put("is_edit", true)
        paramsMap.put("member_ids", memberIds)
        MemberRoleDTO memberRoleDO = new MemberRoleDTO()
        memberRoleDO.setSourceType("project")
        //null
        def memberRoleDOList1 = memberRoleMapper.select(memberRoleDO)

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList1, String, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()

        when: "调用方法"
        memberRoleDO.setMemberType(MemberType.CLIENT.value())
        entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members?is_edit={is_edit}&member_ids={member_ids}", memberRoleDOList1, String, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "PagingQueryUsersByRoleIdOnSiteLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleId = roleDOList.get(0).getId()
        paramsMap.put("role_id", roleId)
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/users?role_id={role_id}", roleAssignmentSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().list.size() != 0
    }

    def "PagingQueryClientsByRoleIdOnSiteLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleId = roleDOList.get(0).getId()
        paramsMap.put("role_id", roleId)
        def clientRoleSearchDTO = new ClientRoleQuery()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/clients?role_id={role_id}", clientRoleSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody() != null
    }

    def "PagingQueryUsersByRoleIdOnOrganizationLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleId = roleDOList.get(0).getId()
        paramsMap.put("role_id", roleId)
        paramsMap.put("organization_id", 1L)
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/users?role_id={role_id}", roleAssignmentSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().list.isEmpty()
    }

    def "PagingQueryClientsByRoleIdOnOrganizationLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleId = roleDOList.get(0).getId()
        paramsMap.put("role_id", roleId)
        paramsMap.put("organization_id", 1L)
        def clientRoleSearchDTO = new ClientRoleQuery()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/clients?role_id={role_id}", clientRoleSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().list.isEmpty()
    }

    def "PagingQueryUsersByRoleIdOnProjectLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleId = roleDOList.get(0).getId()
        paramsMap.put("role_id", roleId)
        paramsMap.put("project_id", projectDO.getId())
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/users?role_id={role_id}", roleAssignmentSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        //只有site用户
        entity.getBody().list.isEmpty()
    }

    def "PagingQueryClientsByRoleIdOnProjectLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleId = roleDOList.get(0).getId()
        paramsMap.put("role_id", roleId)
        paramsMap.put("project_id", projectDO.getId())
        def clientRoleSearchDTO = new ClientRoleQuery()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/clients?role_id={role_id}", clientRoleSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        //只有site用户
        entity.getBody().list.isEmpty()
    }

    @Transactional
    def "DeleteOnSiteLevel"() {
        given: "构造参数列表"
        def paramsMap = new HashMap<String, Object>()
        def roleAssignmentDeleteDTO = new RoleAssignmentDeleteDTO()
        roleAssignmentDeleteDTO.setSourceId(0)
        roleAssignmentDeleteDTO.setMemberType("user")
        roleAssignmentDeleteDTO.setView("userId")
        List<Long> roleIds = new ArrayList<>()
        roleIds.add(roleDOList.get(0).getId())
        roleIds.add(roleDOList.get(1).getId())
        roleIds.add(roleDOList.get(2).getId())
        def map = new HashMap<Long, List<Long>>()
        map.put(userDOList.get(0).getId(), roleIds)
        roleAssignmentDeleteDTO.setData(map)

        when: "调用方法[异常-view不合法]"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/delete", roleAssignmentDeleteDTO, ExceptionResponse, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().getCode().equals("error.member_role.view.illegal")

        when: "调用方法"
        roleAssignmentDeleteDTO.setView("userView")
        entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/delete", roleAssignmentDeleteDTO, Void, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    @Transactional
    def "DeleteOnOrganizationLevel"() {
        given: "构造参数列表"
        def paramsMap = new HashMap<String, Object>()
        def roleAssignmentDeleteDTO = new RoleAssignmentDeleteDTO()
        roleAssignmentDeleteDTO.setSourceId(0)
        roleAssignmentDeleteDTO.setMemberType("user")
        roleAssignmentDeleteDTO.setView("userView")
        List<Long> roleIds = new ArrayList<>()
        roleIds.add(roleDOList.get(0).getId())
        roleIds.add(roleDOList.get(1).getId())
        roleIds.add(roleDOList.get(2).getId())
        def map = new HashMap<Long, List<Long>>()
        map.put(userDOList.get(0).getId(), roleIds)
        roleAssignmentDeleteDTO.setData(map)
        paramsMap.put("organization_id", 1L)

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/delete", roleAssignmentDeleteDTO, Void, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    @Transactional
    def "DeleteOnProjectLevel"() {
        given: "构造参数列表"
        def paramsMap = new HashMap<String, Object>()
        def roleAssignmentDeleteDTO = new RoleAssignmentDeleteDTO()
        roleAssignmentDeleteDTO.setSourceId(0)
        roleAssignmentDeleteDTO.setMemberType("user")
        roleAssignmentDeleteDTO.setView("userView")
        List<Long> roleIds = new ArrayList<>()
        roleIds.add(roleDOList.get(0).getId())
        roleIds.add(roleDOList.get(1).getId())
        roleIds.add(roleDOList.get(2).getId())
        def map = new HashMap<Long, List<Long>>()
        map.put(userDOList.get(0).getId(), roleIds)
        roleAssignmentDeleteDTO.setData(map)
        paramsMap.put("project_id", 1L)

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/delete", roleAssignmentDeleteDTO, Void, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "ListRolesWithUserCountOnSiteLevel"() {
        given: "构造请求参数"
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/users/count", roleAssignmentSearchDTO, List)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        !entity.getBody().isEmpty()
    }

    def "ListRolesWithClientCountOnSiteLevel"() {
        given: "构造请求参数"
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/clients/count", roleAssignmentSearchDTO, List)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        !entity.getBody().isEmpty()
    }

    def "ListRolesWithUserCountOnOrganizationLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("organization_id", 1L)
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/users/count", roleAssignmentSearchDTO, List, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().size() == 2
    }

    def "ListRolesWithClientCountOnOrganizationLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("organization_id", 1L)
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/clients/count", roleAssignmentSearchDTO, List, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody() != null
    }

    def "ListRolesWithUserCountOnProjectLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("project_id", projectDO.getId())
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/users/count", roleAssignmentSearchDTO, List, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().size() == 4
    }

    def "ListRolesWithClientCountOnProjectLevel"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("project_id", projectDO.getId())
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/clients/count", roleAssignmentSearchDTO, List, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody() != null
    }

    def "PagingQueryUsersWithSiteLevelRoles"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/users/roles", roleAssignmentSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().list.size() != 0
    }

    def "PagingQueryClientsWithSiteLevelRoles"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/clients/roles", roleAssignmentSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody() != null
    }

    def "PagingQueryUsersWithOrganizationLevelRoles"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("organization_id", 1L)
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/users/roles", roleAssignmentSearchDTO, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        //自己插入的userDO
        entity.getBody().list.size() != 0
    }

    def "pagingQueryClientsWithOrganizationLevelRoles"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("organization_id", 1L)
        def clientSearch = new ClientRoleQuery()
        clientSearch.setClientName("client")
        clientSearch.setRoleName("管理")

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/clients/roles", clientSearch, PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().list.size() == 0
    }

    def "PagingQueryUsersWithProjectLevelRoles"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("project_id", projectDO.getId())
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/users/roles", roleAssignmentSearchDTO, String, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody() != null
    }

    def "PagingQueryClientsWithProjectLevelRoles"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("project_id", projectDO.getId())
        def roleAssignmentSearchDTO = new RoleAssignmentSearchDTO()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/clients/roles", roleAssignmentSearchDTO, String, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody() != null
    }

    def "GetUserWithOrgLevelRolesByUserId"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("user_id", userDOList.get(0).getId())
        paramsMap.put("organization_id", 1L)

        when: "调用方法"
        def entity = restTemplate.getForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/users/{user_id}", List, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        //!entity.getBody().isEmpty()
    }

    def "GetUserWithProjLevelRolesByUserId"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("user_id", userDOList.get(0).getId())
        paramsMap.put("project_id", projectDO.getId())

        when: "调用方法"
        def entity = restTemplate.getForEntity(BASE_PATH + "/projects/{project_id}/role_members/users/{user_id}", List, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().isEmpty()
    }

    def "DownloadTemplatesOnSite"() {
        when: "调用方法"
        def entity = restTemplate.getForEntity(BASE_PATH + "/site/role_members/download_templates", Resource)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().exists()
    }

    def "DownloadTemplatesOnOrganization"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("organization_id", 1L)

        when: "调用方法"
        def entity = restTemplate.getForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/download_templates", Resource, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().exists()
    }

    def "DownloadTemplatesOnProject"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("project_id", projectDO.getId())

        when: "调用方法"
        def entity = restTemplate.getForEntity(BASE_PATH + "/projects/{project_id}/role_members/download_templates", Resource, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
        entity.getBody().exists()
    }

    def "Import2MemberRoleOnSite"() {
        given: "构造请求参数"
        MultipartFile file = null
        def paramsMap = new HashMap<String, Object>()

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/site/role_members/batch_import", file, Void, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "Import2MemberRoleOnOrganization"() {
        given: "构造请求参数"
        MultipartFile file = null
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("organization_id", 1L)

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/organizations/{organization_id}/role_members/batch_import", file, Void, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "Import2MemberRoleOnProject"() {
        given: "构造请求参数"
        MultipartFile file = null
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("project_id", projectDO.getId())

        when: "调用方法"
        def entity = restTemplate.postForEntity(BASE_PATH + "/projects/{project_id}/role_members/batch_import", file, Void, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "LatestHistoryOnSite"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("user_id", userDOList.get(0).getId())

        when: "调用方法"
        def entity = restTemplate.getForEntity(BASE_PATH + "/site/member_role/users/{user_id}/upload/history", UploadHistoryDTO, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "LatestHistoryOnOrganization"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("user_id", userDOList.get(0).getId())
        paramsMap.put("organization_id", 1L)

        when: "调用方法"
        def entity = restTemplate.getForEntity(BASE_PATH + "/organizations/{organization_id}/member_role/users/{user_id}/upload/history", UploadHistoryDTO, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "LatestHistoryOnProject"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("user_id", userDOList.get(0).getId())
        paramsMap.put("project_id", projectDO.getId())

        when: "调用方法"
        needClean = true
        def entity = restTemplate.getForEntity(BASE_PATH + "/projects/{project_id}/member_role/users/{user_id}/upload/history", UploadHistoryDTO, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "queryAllClients"() {
        given: "构造请求参数"
        def paramsMap = new HashMap<String, Object>()
        paramsMap.put("size", 10)
        paramsMap.put("page", 0)

        when: "调用方法"
        needClean = true
        def entity = restTemplate.getForEntity(BASE_PATH + "/all/clients", PageInfo, paramsMap)

        then: "校验结果"
        entity.statusCode.is2xxSuccessful()
    }

    def "queryAllUsers"() {
        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        PageRequest pageRequest = new PageRequest(1, 20)

        when: "调用方法"
        def result = controller.queryAllUsers(pageRequest, 1L, "param")
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }

    def "pagingQueryUsersOnSiteLevel"() {

        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        PageRequest pageRequest = new PageRequest(1, 20)

        when: "调用方法"
        def result = controller.pagingQueryUsersOnSiteLevel(pageRequest, 1L, "123afa@qq.com", "param")
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }

    def "pagingQueryUsersWithSiteLevelRolesWithDeveloper"() {

        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        PageRequest pageRequest = new PageRequest(1, 20)
        RoleAssignmentSearchDTO roleAssignmentSearchDTO = new RoleAssignmentSearchDTO();
        roleAssignmentSearchDTO.setLoginName("tom")
        def params = ['1', '2', '3', '4'] as String[]
        roleAssignmentSearchDTO.setParam(params)
        roleAssignmentSearchDTO.setRealName("root")
        roleAssignmentSearchDTO.setRoleName("admin")

        when: "调用方法"
        def result = controller.pagingQueryUsersWithSiteLevelRolesWithDeveloper(pageRequest, roleAssignmentSearchDTO)
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }


    @Transactional
    def "updateUserRolesOnSiteLevel"() {

        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        List<RoleDTO> roleDTOList = new ArrayList<>();
        List<Long> ids = new ArrayList<>()
        ids.add(3L)

        List<UserDTO> userDTOList = new ArrayList<>();
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("32rr@qq.com")
        userDTO.setRealName("tom")
        userDTOList.add(userDTO)

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(1L)
        roleDTO.setName("admin")
        roleDTO.setCode("001")
        roleDTO.setResourceLevel("阿发")
        roleDTOList.add(roleDTO)

        when: "调用方法"
        def result = controller.updateUserRolesOnSiteLevel(1L, roleDTOList)
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }

    @Transactional
    def "updateUserRolesOnOrganizationLevel"() {
        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        List<RoleDTO> roleDTOList = new ArrayList<>();
        List<Long> ids = new ArrayList<>()
        ids.add(3L)

        List<UserDTO> userDTOList = new ArrayList<>();
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("32rr@qq.com")
        userDTO.setRealName("tom")
        //userDTO.setRoles()
        userDTOList.add(userDTO)

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(4L)
        roleDTO.setName("root")
        roleDTO.setCode("role/site/default/rolemember1")
        roleDTO.setResourceLevel("site")
        roleDTO.setRoleIds(ids)
        roleDTO.setUsers(userDTOList)
        roleDTOList.add(roleDTO)

        when: "调用方法"
        def result = controller.updateUserRolesOnOrganizationLevel(1L, 1L, roleDTOList)
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }

    @Transactional
    def "updateUserRolesOnProjectLevel"() {
        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        //构造project
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(20L)
        projectDTO.setName("汉得研发")
        projectDTO.setCode("ssm")

        List<RoleDTO> roleDTOList = new ArrayList<>();
        List<Long> ids = new ArrayList<>()
        ids.add(3L)

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(8L)
        roleDTO.setName("root")
        roleDTO.setCode("role/site/default/rolemember1")
        roleDTO.setResourceLevel("organization")
        roleDTO.setRoleIds(ids)
        roleDTO.setEnabled(true)
        //roleDTO.setUsers(userDTOList)
        roleDTOList.add(roleDTO)

        when: "调用方法"
        def m = projectMapper.insert(projectDTO)

        then: "校验结果"
        m > 0

        when: "调用方法"
        def result = controller.updateUserRolesOnProjectLevel(20L, 1L, roleDTOList)

        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }

    @Transactional
    def "assignUsersRolesOnSiteLevel"() {
        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        List<Long> memberIds = new ArrayList<>()
        memberIds.add(1L)
        List<Long> roleIds = new ArrayList<>()
        for (int i = 1; i <= 3; i++) {
            roleIds.add((long) i)
        }

        when: "调用方法"
        def result = controller.assignUsersRolesOnSiteLevel(memberIds, roleIds)
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }

    @Transactional
    def "assignUsersRolesOnOrganizationLevel"() {
        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        List<Long> memberIds = new ArrayList<>()
        memberIds.add((long) 1)
        List<Long> roleIds = new ArrayList<>()
        for (int i = 4; i < 6; i++) {
            roleIds.add((long) i)
        }
        when: "调用方法"
        def result = controller.assignUsersRolesOnOrganizationLevel(1L, memberIds, roleIds)
        then: "校验结果"
        result.statusCode.is2xxSuccessful()

    }

    @Transactional
    def "assignUsersRolesOnProjectLevel"() {
        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        //构造project
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(30L)
        projectDTO.setName("汉得研发")
        projectDTO.setCode("ssm")

        List<Long> memberIds = new ArrayList<>()
        List<Long> roleIds = new ArrayList<>()
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setResourceLevel("project")
        memberIds.add(1L)
        roleIds.add(8L)

        when: "调用方法"
        def m = projectMapper.insert(projectDTO)

        then: "校验结果"
        m > 0

        when: "调用方法"
        def result = controller.assignUsersRolesOnProjectLevel(30L, memberIds, roleIds)
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }

    def "import2MemberRoleOnSite"() {
        given: "构造请求参数"
        RoleMemberController controller = new RoleMemberController(null, userService, null, null, null, null)
        //构造project
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(30L)
        projectDTO.setName("汉得研发")
        projectDTO.setCode("ssm")

        List<Long> memberIds = new ArrayList<>()
        List<Long> roleIds = new ArrayList<>()
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setResourceLevel("project")
        memberIds.add(1L)
        roleIds.add(8L)

        when: "调用方法"
        def m = projectMapper.insert(projectDTO)

        then: "校验结果"
        m > 0

        when: "调用方法"
        def result = controller.assignUsersRolesOnProjectLevel(30L, memberIds, roleIds)
        then: "校验结果"
        result.statusCode.is2xxSuccessful()
    }
}