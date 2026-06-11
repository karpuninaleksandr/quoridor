local authors = {}
local advisors = {}
local affiliations = {}
local last_role = ''

local function extract_name_parts(name)
   local result = {}
   local initials = {}
   for token in string.gmatch(name, "[^%s]+") do
      if next(result) == nil then
         result['surname'] = token
      else
         table.insert(initials, string.sub(token, 1, utf8.offset(token, 2) - 1) .. '.')
      end
   end
   result['initials'] = table.concat(initials, '\\,')
   return result
end

local function join(tab, separator, fun)
   local result = {}
   for i,element in ipairs(tab) do
      table.insert(result, fun(i, element))
   end
   return table.concat(result, separator)
end

local function determine_affiliation_id(in_affiliation)
   for i, affiliation in ipairs(affiliations) do
      if in_affiliation == affiliation then
         return i
      end
   end
   table.insert(affiliations, in_affiliation)
   return #affiliations
end

local function affiliation_index(i)
   if #affiliations < 2 then
      return ''
   else
      return '\\textsuperscript{' .. i .. '}'
   end
end

-- Public functions

function add_author(name)
   table.insert(authors, extract_name_parts(name))
   last_role = 'author'
end

function set_affiliation(affiliation)
   local affiliation_id = determine_affiliation_id(affiliation)
   if last_role == 'author' then
      authors[#authors]['affiliation'] = affiliation_id
   else
      advisors[#advisors]['affiliation'] = affiliation_id
   end
end

function set_author_email(email)
   authors[#authors]['email'] = email
end

function add_advisor(post, name)
   table.insert(advisors, extract_name_parts(name))
   advisors[#advisors]['post'] = post
   last_role = 'advisor'
end

function output_authors()
   return join(authors, ', ', function (i, author)
                  return author['initials'] .. '~' .. author['surname'] .. affiliation_index(author['affiliation']) end)
end

function output_authors_for_copyright()
   return join(authors, ', ', function (i, author)
                  return author['surname'] .. '~' .. author['initials'] end)
end

function output_authors_emails()
   return join(authors, ', ', function (i, author)
                  return '\\href{mailto:' .. author['email'] .. '}{' .. string.gsub(author['email'], '_', '\\_') .. '}' end)
end

function output_affiliations()
   return join(affiliations, '\\\\', function (i, affiliation) return affiliation_index(i) .. affiliation end)
end

function output_advisors()
   local title = ''
   local result = {}
   if #advisors == 0 then
      return ''
   elseif #advisors == 1 then
      title = 'Научный руководитель: '
   else
      title = 'Научные руководители: '
   end
   return title .. join(advisors, ', ', function(i, advisor)
                           return advisor['post'] .. ' ' .. advisor['initials'] .. '~' .. advisor['surname'] .. affiliation_index(advisor['affiliation']) end)
end
