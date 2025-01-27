# NOTE: This file is auto generated by OpenAPI Generator 7.0.0-SNAPSHOT (https://openapi-generator.tech).
# Do not edit this file manually.

defmodule OpenapiPetstore.Model.Pet do
  @moduledoc """
  
  """

  @derive Jason.Encoder
  defstruct [
    :id,
    :category,
    :name,
    :photo_urls,
    :tags,
    :status
  ]

  @type t :: %__MODULE__{
    :id => integer() | nil,
    :category => OpenapiPetstore.Model.Category.t | nil,
    :name => String.t,
    :photo_urls => [String.t],
    :tags => [OpenapiPetstore.Model.Tag.t] | nil,
    :status => String.t | nil
  }
end

